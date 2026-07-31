# Release 1 reconnaissance: external providers and transaction ownership

Date: 2026-07-31 (Asia/Kolkata)

Scope: follow-up to commit `86ba3d87da06d50895c3baf10c4b9aa60c4de867`. This report covers only Google Places public-review calls and the Google connectivity provider.

## Confirmed call graph

- Project, builder and company public-review admin controllers call the Spring-proxied `PublicReviewService.syncGoogleReviews(...)` entry point.
- Project public-review search calls the Spring-proxied `PublicReviewService.searchGooglePlaces(...)` entry point.
- Admin and dashboard connectivity controllers call the Spring-proxied `ProjectConnectivityService.providerSearch(...)` entry point.
- `PublicReviewServiceImpl` delegates through `ReviewPlaceProvider` / `GoogleReviewPlaceProvider` to `GooglePlacesClient`.
- `ProjectConnectivityServiceImpl` delegates through `NearbyPlaceProvider` to `GoogleNearbyPlaceProvider`.
- No inspected caller wraps these controller-to-service calls in another application transaction.

## Transaction boundaries before Release 1

- `syncGoogleReviews(...)` is `@Transactional`; validation reads, provider HTTP, sample replacement, summary writes and status writes share one transaction.
- `searchGooglePlaces(...)` is `@Transactional(readOnly = true)`; project lookup and provider HTTP share one transaction.
- `providerSearch(...)` is `@Transactional(readOnly = true)`; project lookup, provider HTTP and duplicate-detection queries share one transaction.
- Consequently a delayed provider retains a Hikari connection for its entire delay.

## Current timeout and failure behavior

- Public-review `GooglePlacesClient` uses `HttpClient.newHttpClient()` and has no configured connect or request timeout.
- Connectivity `GoogleNearbyPlaceProvider` applies one `google.maps.places.timeout-ms` value to both connect and read timeout. Production defaults it to 8000 ms.
- Public-review non-2xx, transport and parse failures become `IllegalStateException` and normally map to an uncontrolled HTTP 500.
- Connectivity `RestClientException` becomes `IllegalStateException` and normally maps to an uncontrolled HTTP 500.
- Public-review sync marks `FAILED` for generic runtime failures, but its explicit `ResponseStatusException` branch does not mark `FAILED`.
- Error messages do not expose the API key, but timeout, availability and upstream-response categories are not distinguished.

## Constrained-pool reproduction

A temporary standalone Java reproduction used PostgreSQL 16 Testcontainers, Hikari maximum/minimum 1, Spring `DataSourceTransactionManager`, and the current transaction shape: perform a database read, block in a simulated provider, then return.

Observed while the provider was blocked:

```text
baseline-during-provider active=1 idle=0 pending=0
```

Observed immediately after release:

```text
baseline-after-provider active=0 idle=1 pending=0
```

This proves that a provider delay inside the current service transaction retains the sole connection. With ten such calls, the production pool of ten can be occupied without any slow SQL or connection leak.

## Release 1 implementation constraints

- Provider orchestration must suspend any ambient transaction.
- Database preparation, result persistence and failure-status persistence must use separate short transactions.
- Provider calls must occur between those transactions.
- Connect and response/request timeout values must be externalized and validated.
- Timeout must map to HTTP 504; unavailable/misconfigured provider to HTTP 503; upstream/transport/response failures to HTTP 502.
- The production Hikari maximum remains 10.
- PostgreSQL integration tests must observe zero active Hikari connections while delayed providers are blocked.

## Implemented boundary and validation evidence

- The three provider entry points now use `Propagation.NOT_SUPPORTED`, which suspends an ambient caller transaction.
- A shared `ExternalProviderTransactions` component owns the short read/write database phases. Only scalar preparation data crosses into provider I/O; persistence re-reads managed entities in a new transaction.
- Google sync failure status is persisted in a separate short transaction without replacing the original provider or persistence exception.
- Both clients now use validated, configuration-driven connect/read/request bounds. The JDK HTTP client exposes connect plus full-response timeouts, so the smaller read/request value is enforced as the response-exchange deadline.
- Controlled mapping is: timeout 504, unavailable/disabled/misconfigured 503, and upstream transport/non-2xx/parse failure 502. Upstream bodies, URLs and credentials are excluded from client-visible messages.
- Production Hikari maximum remains 10.

The PostgreSQL 16 Testcontainers test uses Hikari maximum/minimum 1. While each provider was deliberately blocked, it observed:

```text
delayed-provider flow=public-review active=0 idle=1 pending=0
delayed-provider flow=connectivity active=0 idle=1 pending=0
```

Focused tests: 20 passed. Full suite: 853 passed, 0 failures, 0 errors, 0 skipped.
