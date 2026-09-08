# Release 4B2 reconnaissance: rate-limit memory, identity, proxy and abuse hardening

Date: 2026-07-31
Scope: in-process mobile/public rate limiting only. Public response caching, infrastructure, Redis, broader JWT optimization beyond eliminating duplicate per-request parsing, and staging capacity work are excluded.

## Release 4B1 slow-log follow-up

`ApiRequestLoggingFilter` emitted every slow request through `LOGGER_API_RELIABLE`, including successful responses. Because that logger writes synchronously, a merely slow but successful request could add more request-thread disk latency. The narrow follow-up routes successful slow markers through the already bounded/non-blocking API INFO appender, adds `slow=true`, and preserves duration, route, status and request ID through structured fields/MDC. Exceptions and non-success responses remain reliable; the actual exception/5xx event is never sampled or discarded. Focused routing tests were added before beginning 4B2.

## Files and flows inspected completely

- `InMemoryRateLimitService`, `RateLimitService`, all rate-limit models, enums and exceptions.
- `RateLimitingFilter`, `CachedBodyHttpServletRequest`, and servlet auto-registration suppression.
- `RateLimitProperties`, all 45 policy configurations, and `RateLimitPolicyResolver` route mappings.
- `RateLimitKeyResolver`, `ClientIpResolver`, and every key construction branch.
- `SecurityConfig`, `JwtRequestFilter`, `JwtTokenUtil`, mobile identity snapshots, and filter ordering.
- Guest-session controller, DTOs, entity, repository, transactional service, token creation and token validation.
- Existing rate-limit unit, integration, security-ordering, logging-safety, configuration and response-contract tests.
- Production smoke/checklist/mobile integration documentation and proxy assumptions.

The worktree contains unrelated changes to `RateLimitPolicyResolver` and its test for a new public-city route. Those changes are not part of 4B2 and must remain unstaged; 4B2 does not require editing the route table.

## Current architecture and semantics

- One Caffeine `Cache<String, Bucket>` stores every dimension for every policy.
- Default maximum: 200,000 entries; expiry: 120 minutes after access.
- `Cache.get(key, mappingFunction)` gives atomic per-key bucket creation, but cache statistics and eviction listeners are disabled.
- A request must pass every configured dimension. Previously consumed dimensions are refunded if a later dimension blocks.
- Missing key material fails open for only that dimension. Missing policy configuration also fails open after a warning; startup validation normally prevents that state.
- No database call is made by the rate-limit service. The filter runs after `JwtRequestFilter` and before controller dispatch in the mobile/public security chain. Dashboard/admin routes use a different chain and do not run this filter.

## Key-cardinality audit

| Key type | Current material | Cardinality/control finding |
|---|---|---|
| `PHONE` | normalized phone; malformed input falls back to lower-cased raw input | Client controlled; arbitrary malformed values create entries. |
| `IP` | `ClientIpResolver` output | Direct XFF is ignored, but trusted-proxy XFF is neither parsed nor normalized and can contain arbitrary strings. |
| `IP_AND_TOKEN` | IP plus SHA-256 of refresh token | Raw token is not retained, but every random invalid refresh token creates an entry. |
| `IP_AND_INSTALLATION` | IP plus request-body installation ID | Client controlled; the guest-creation endpoint trusts it for key cardinality before server issuance. |
| `IP_OR_USER` | JWT user ID when reparsing succeeds, otherwise IP | Authenticated users are independent; valid guests have no user ID and collapse to the NAT IP. Invalid bearer values fall back to one IP key. |
| `IP_AND_DEVICE` | IP plus client device ID/query value | Client controlled and not authenticated. |
| `IP_AND_QUERY` | IP plus SHA-256 of normalized query | Client controlled; unique queries create entries. |
| `BODY_FINGERPRINT` | SHA-256 of canonical JSON | Client controlled; unique bodies create entries in the same cache as abuse buckets. |

Path/query resource IDs do not otherwise enter keys. Raw access/refresh tokens are not logged or used directly, but hashes still create cardinality. The current filter reparses the access JWT to obtain `userId` even though authentication has already run.

## Authentication and guest identity findings

- A valid mobile user is loaded/validated by `JwtRequestFilter`; `IP_OR_USER` then reparses the bearer token for its user ID.
- A server-issued guest JWT is signature/expiry/subject validated and installed as a `ROLE_GUEST` authentication whose principal is installation ID. The rate limiter ignores that principal and falls back to IP.
- Guest validation does not query `guest_sessions`; this is current security behavior and 4B2 must not introduce a database lookup.
- Invalid bearer tokens are classified/logged by the JWT filter, but that classification is not passed to rate limiting. The rate limiter reparses and falls back to IP.
- Arbitrary device or installation identifiers from unauthenticated request parameters/bodies are not proof of identity.

## Client-IP and proxy findings

- XFF is trusted only when `request.getRemoteAddr()` exactly matches a configured proxy. Defaults are IPv4/IPv6 loopback for local nginx.
- A direct client cannot spoof XFF under the current implementation.
- A trusted peer can supply `attacker-controlled-not-an-ip`; the resolver accepts it as a key.
- IPv4/IPv6 textual equivalents are not canonicalized. IPv4-mapped IPv6, expanded/compressed IPv6, ports, brackets, zone IDs and invalid text are not handled explicitly.
- Repository documentation assumes nginx is local but contains no nginx/systemd configuration proving that nginx overwrites rather than appends client-supplied XFF. Deployment verification remains necessary; no infrastructure change is authorized.

## Baseline retained-memory benchmark

Method: isolated committed-tree JVM, Caffeine 3.2.0, Bucket4j 8.14.0, forced-GC before/after retained-heap approximation, one Bucket4j bucket per key. This is an estimate rather than an object-graph/JFR production measurement.

| Entries | Cache size | Retained bytes | Approx. bytes/entry | Creation ns/entry |
|---:|---:|---:|---:|---:|
| 0 | 0 | 595,568 framework/noise | n/a | n/a |
| 1,000 | 1,000 | 585,216 | 585.2 | 58,538.9 |
| 10,000 | 10,000 | 5,002,328 | 500.2 | 8,184.2 |
| 50,000 | 50,000 | 25,326,688 | 506.5 | 4,576.1 |
| 100,000 | 100,000 | 55,982,440 | 559.8 | 1,330.0 |

At the configured 200,000 maximum, linear extrapolation is approximately 107 MiB retained, excluding temporary allocation and measurement uncertainty. A 100,000-write eviction run against a 1,000-entry cache completed in 129.3 ms (about 1.29 microseconds/write) and cleaned to 1,000 entries.

The final design budget is 16 MiB for all rate-limit caches. Using the conservative measured 585 bytes/entry, a 20,000-entry combined bound is about 11.2 MiB, leaving roughly 4.8 MiB (30%) for Caffeine policy overhead, counters, transient writes and measurement error.

## Baseline behavior simulations

- Ten authenticated callers behind one NAT on an IP-only public policy with capacity 3: 3 allowed, 7 rejected, 1 cache entry. Authentication does not help for the many production policies still configured as `IP`.
- Ten valid guests behind one NAT on `IP_OR_USER` with capacity 3: 3 allowed, 7 rejected, 1 cache entry because guest identity is ignored.
- 10,000 unique invalid bearer values behind one IP: 1 `IP_OR_USER` entry, but each request incurs failed token parsing before the IP fallback.
- 10,000 unique invalid refresh-token body values behind one IP: 10,000 `IP_AND_TOKEN` entries.
- Direct XFF spoof: ignored and resolved to the direct peer `198.51.100.7`.
- Trusted-loopback malformed XFF: accepted verbatim as `attacker-controlled-not-an-ip`.

## Root risks

1. The 200,000 bound is far too expensive for a small shared JVM at the measured per-entry footprint.
2. Most public policies use one NAT-wide IP bucket, including valid authenticated/guest callers.
3. Guest principals are not used even after valid server-issued JWT authentication.
4. Attacker-controlled supplemental dimensions share the cache with IP protection; churn can evict the very IP bucket intended to prevent eviction-based bypass.
5. Invalid refresh tokens, arbitrary installation/device IDs, queries and bodies can consume cache cardinality.
6. Trusted XFF values are not validated/canonicalized; IPv6 aliases split identities.
7. There are no observable cache hit/miss/eviction/creation or allow/reject/fallback metrics.
8. The 429 body is a custom partial envelope and omits timestamp, path and request ID used by the standard `ApiError` contract.

## Implementation direction

- Add an explicit immutable identity result and `RateLimitIdentityResolver` consuming only authentication outcomes established by `JwtRequestFilter` plus a canonical trusted client IP.
- User primary identity: immutable user ID. Valid guest primary identity: server-validated guest-session ID. Unknown and invalid authentication: fixed classifications scoped by canonical IP; never token-derived.
- Use separate primary/supplemental and IP-abuse Caffeine caches so attacker-controlled content churn cannot evict abuse protection.
- Bound each cache at 10,000 entries (20,000 combined) with shorter explicit expiry, atomic loading, and cache statistics.
- Replace NAT-wide public `IP` primary limits with principal-aware primary limits and add separately configured, more generous `IP_ABUSE` limits. Auth/bootstrap policies retain phone where justified but use fixed IP abuse rather than token/device/installation cardinality.
- Validate/canonicalize IPv4/IPv6 and honor XFF only from normalized configured trusted proxies.
- Add aggregate low-cardinality metrics and a standard 429 envelope retaining the existing retry/policy fields.
- Define security/write policies as fail-closed for missing mandatory identity or limiter failure; public reads fail open while still recording a fallback metric.
