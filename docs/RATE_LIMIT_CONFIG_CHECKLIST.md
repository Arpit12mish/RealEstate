# Rate Limit Configuration Checklist

Run through this before any deploy that touches `com.brandPitara.sfs.ratelimit`
or its configuration. Each item states how to verify it and what "good" looks
like.

## Configuration tracking

- [x] **`application-rate-limit.yml` is tracked in git.**
  Verify: `git ls-files src/main/resources/application-rate-limit.yml` returns
  the path (not empty).

- [x] **`application-prod.yml` imports it.**
  Verify: `grep "application-rate-limit.yml" src/main/resources/application-prod.yml`.
  Also enforced by `RateLimitConfigLoadingTest.prodProfileDeclaresTheRateLimitConfigImport`.

- [x] **`application-test.yml` imports it.**
  Verify: `grep "application-rate-limit.yml" src/test/resources/application-test.yml`.
  Also enforced by `RateLimitConfigLoadingTest.testProfileDeclaresTheRateLimitConfigImport`.

- [x] **No rate-limit config lives only in the gitignored `application.yml`.**
  `application.yml` only re-declares the same `spring.config.import` pointer
  (for local dev convenience) - it holds no policy config of its own. Verify:
  `grep -A2 "rate-limit" src/main/resources/application.yml` should show only
  a comment, not a `policies:` block.

- [x] **No secrets in rate-limit config.**
  `application-rate-limit.yml` contains only limits, key types, and toggles -
  no credentials, tokens, or connection strings. Verify:
  `grep -iE "password|secret|token|api-key|postgresql://" src/main/resources/application-rate-limit.yml`
  should return nothing (the one historical false-positive was the field name
  `refillTokens`, not a real token value - check any new match by hand before
  assuming it's a real secret).

## Runtime tuning

- [x] **Both bucket caches have independent max sizes and TTLs.**
  The primary cache is capped at 10,000 entries/30 minutes and the IP-abuse
  cache at 10,000 entries/60 minutes. They are environment-overridable via
  `RATE_LIMIT_PRIMARY_BUCKET_CACHE_MAX_SIZE`, `RATE_LIMIT_ABUSE_BUCKET_CACHE_MAX_SIZE`,
  `RATE_LIMIT_PRIMARY_BUCKET_CACHE_EXPIRE_MINUTES`, and
  `RATE_LIMIT_ABUSE_BUCKET_CACHE_EXPIRE_MINUTES`. The split prevents primary-key
  churn from evicting IP abuse protection. The combined 20,000-entry cap was
  selected from a 16 MiB heap budget: mature 100,000-entry measurements retained
  about 524 bytes per entry; the final split retained about 9.8 MiB, or roughly
  12.7 MiB after a 30% safety margin.

- [x] **Max cached body bytes is configured.**
  `sfs.rate-limit.max-cached-body-bytes` (32 KB default,
  `RATE_LIMIT_MAX_CACHED_BODY_BYTES` override). Requests to a body-aware
  policy (OTP request/verify/refresh, guest session, location resolve,
  calculator write) whose body exceeds this are rejected with `413` before
  the real controller ever runs - confirmed by
  `RateLimitingFilterIntegrationTest.oversizedRequestBodyReturns413BeforeControllerRuns`
  and the calculator-specific equivalent.

- [x] **Trusted proxy config reviewed.**
  `sfs.rate-limit.trusted-proxies` defaults to loopback
  (`127.0.0.1`, `::1`, `0:0:0:0:0:0:0:1`), matching the current single-EC2 +
  local-nginx deployment. `X-Forwarded-For` is only honored when the direct
  TCP peer is in this list - otherwise `ClientIpResolver` falls back to
  `request.getRemoteAddr()`, so a request cannot spoof its rate-limit IP by
  sending an arbitrary header directly to the app. **If the deployment
  topology ever changes** (e.g. an external load balancer or CDN is added in
  front of nginx), this list must be updated to include that hop's real
  address, or `X-Forwarded-For` spoofing becomes possible again.

- [x] **`sfs.rate-limit.enabled` / `default-enabled` reviewed.**
  Both default to `true`. `enabled` (env: `RATE_LIMIT_ENABLED`) is the global
  kill switch - set to `false` only as a last-resort incident response if rate
  limiting itself is misbehaving in production, since it disables enforcement
  for every policy at once.

## Coverage

- [x] **Dashboard intentionally excluded.**
  `RateLimitingFilter` is wired only into `SecurityConfig.appFilterChain`;
  `dashboardFilterChain` (`securityMatcher("/api/dashboard/**", "/api/admin/**")`)
  never runs it - dashboard/admin requests are structurally unreachable by
  this filter, not merely excluded by route pattern. Verify:
  `RateLimitPolicyResolverTest`'s dashboard/admin tests, and
  `RateLimitingFilterIntegrationTest.dashboardRouteIsUntouchedRegardlessOfVolume`.

- [x] **Mobile/public API coverage is complete** as of the latest phase - see
  the coverage matrices in the phase-by-phase implementation history. No named
  mobile/public route group remains unrated.

## Response contract & logging safety

- [x] **429 response contract is consistent across all policy categories.**
  Status `429`, `Retry-After` header, and a JSON body with
  `timestamp`/`status`/`error`/`message`/`retryAfterSeconds`/`policy`/`path`/
  `requestId` - proven for mobile
  auth, public GET, public POST/body-aware, and authenticated mobile policies
  by `RateLimitResponseContractTest`.

- [x] **Blocked-request logs never contain raw sensitive values.**
  No raw phone number, OTP code, refresh token, JWT, request body, calculator
  body, or email - proven by `RateLimitLoggingSafetyTest` and the
  logging-safety tests embedded in `RateLimitingFilterIntegrationTest`. Log
  lines contain only `policy`, `method`, `path`, `keyType`, `identityType`,
  and `retryAfterSeconds`.

## Metrics

The limiter publishes cache size/hits/misses/evictions, bucket creation,
allowed/rejected decisions, identity fallbacks, invalid-auth rejections and
enforcement failures. Tags are restricted to bounded cache, policy, identity
type and failure-mode enums. IPs, user/session IDs, phone numbers and token
material are never metric tags.
