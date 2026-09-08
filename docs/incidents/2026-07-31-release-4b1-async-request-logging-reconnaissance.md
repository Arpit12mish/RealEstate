# Release 4B1 reconnaissance: bounded asynchronous request logging

Date: 2026-07-31
Scope: request logging only; no rate-limit, public-cache, infrastructure, JWT parsing, or staging-capacity changes.

## Test-count reconciliation completed before Release 4B1

The historical counts were produced from different source sets, not from a regression-test deletion:

- Commit `58a2c047cc7929d24af014c5680532f2897ea9d4` contains 108 tracked test files. An exported clean tree ran 759 tests: 0 failures, 0 errors, 0 skipped.
- Commit `0ce35a957f0f45fc7b31f350bb786fc3ddd54f11` contains 113 tracked test files. Its exported clean tree ran 780 tests: 0 failures, 0 errors, 0 skipped.
- The Release 4A commit adds exactly 21 tests: 19 tests in five new classes and one new test each in `CurrentUserServiceTest` and `ProfileServiceImplTest`.
- `git diff --name-status` shows no deleted test source between the commits. `pom.xml` is unchanged. Neither committed tree contains JUnit `@Disabled`/`@Ignore`, assumptions, Maven test skipping, or Surefire test include/exclude changes. The only `<excludes>` element is the unchanged Spring Boot packaging exclusion for Lombok.
- The previously reported Release 3 count of 861 was a run of the shared dirty worktree, which contained 102 additional uncommitted tests: `759 + 102 = 861`. The later dirty-worktree Release 4A run similarly reported `780 + 102 = 882`. The isolated Release 4A count of 780 is the correct committed-tree count.

## Files and paths inspected completely

- `src/main/resources/logback-spring.xml`
- `src/main/java/com/brandPitara/sfs/observability/ApiRequestLoggingFilter.java`
- `src/main/java/com/brandPitara/sfs/observability/CorrelationIdFilter.java`
- `src/main/java/com/brandPitara/sfs/observability/LoggingConstants.java`
- `src/main/java/com/brandPitara/sfs/observability/LogEvents.java`
- `src/main/java/com/brandPitara/sfs/observability/LogSanitizer.java`
- `src/main/java/com/brandPitara/sfs/exception/GlobalExceptionHandler.java`
- Mobile and dashboard JWT filters, authentication entry points, and dashboard access-denied handler.
- Dashboard action/login audit implementations.
- Refresh-token and Instagram schedulers.
- Google review/connectivity provider adapters and external-provider exception mapping.
- Production/local application logging configuration, production logging documentation, EC2 deployment documentation, and existing logging/redaction tests.
- Repository-wide logging calls and references to Authorization, JWT/access/refresh tokens, OTPs, passwords, API keys, credentials, phone numbers, and request bodies.

## Current logger and appender topology

All appenders are synchronous `ConsoleAppender` or `RollingFileAppender` instances.

| Logger/category | Additivity | Destinations | Current overload behavior |
|---|---:|---|---|
| `com.brandPitara.sfs.observability.api` | false | `sfs-api.log` | Request thread formats and writes every INFO/WARN/ERROR event synchronously. |
| `com.brandPitara.sfs.observability.security` | false | `sfs-security.log` | Synchronous, isolated from API/root. |
| `com.brandPitara.sfs.observability.audit` | false | `sfs-audit.log` | Synchronous, isolated from API/root. |
| root | n/a | console, `sfs-app.log`, ERROR-filtered `sfs-error.log` | Synchronous; ERROR events intentionally appear in app, error, and console destinations. |

API failures currently remain only in `sfs-api.log` because the API logger has `additivity=false`; they do not reach the root ERROR file. Security and audit events do not share the API path. There is no queue, loss policy, queue metric, discard count, or bounded shutdown-drain policy.

All rolling files remain bounded by size, history, and total-size caps. API/app files retain 14 days, errors 30 days, security 60 days, and audit 180 days.

## Duplication and journald

- Within Logback, additivity is explicitly false for API/security/audit, so those events are not duplicated through root.
- Root ERROR events are intentionally copied to console, app, and error appenders, but not duplicated within one destination.
- The repository contains no systemd unit or `StandardOutput`/`StandardError` setting. Production is documented as a systemd `sfs` service. With systemd's default standard-output handling, console output would normally be captured by journald, but that cannot be confirmed from this repository. The deployed unit must be checked with `systemctl cat sfs` and `systemctl show sfs -p StandardOutput -p StandardError` before rollout. No infrastructure change is authorized in 4B1.

## Request-log contract

The filter runs before Spring Security and emits exactly one terminal request event, excluding OPTIONS, favicon, successful health, and ERROR redispatch. It currently preserves:

- correlation ID through MDC;
- method, sanitized request URI, status, duration, principal classification, user/role MDC values;
- response size when `Content-Length` exists;
- exception class and sanitized exception message;
- separate slow-request warning.

The current event does not record Spring MVC's best-matching route template even when the request attribute is available. 4B1 must add a low-cardinality `route` field while retaining the existing `path` field/API contract.

## Sensitive-data findings

Positive controls:

- The request filter never reads request bodies, Authorization, or Cookie headers.
- Sensitive query keys are replaced with `****`; paths and exception messages are newline-stripped and length-bounded.
- Hibernate bind/entity-dump loggers and JDK HTTP-client header logging are disabled in production.
- JWT filters log classifications and fixed messages, not token values.
- External-provider exceptions intentionally omit URLs, bodies, and credentials.

Finding requiring correction:

- `FakeOtpService`, active only under `local`/`local-fake-otp`, logs the fixed OTP, submitted OTP, and full phone number. This violates the stated never-log policy even though it is not a production bean. 4B1 will remove those values rather than relying on profile isolation.

Residual controls to retain:

- Twilio logging masks phone numbers and does not log submitted OTP values. Third-party exception messages remain a residual vendor-controlled text risk and should continue through reliable error logging without adding request bodies or credentials.

## Baseline benchmark

Harness: `RequestLoggingBenchmark`, PostgreSQL 16 Testcontainers, Hikari maximum 3, 2,000 measured requests per scenario after warmup. The lightweight endpoint performs no database work; the representative database endpoint executes one stable PostgreSQL query. The production-equivalent API logger is non-additive and attached to one synchronous file appender. CPU is process CPU divided by wall time and can exceed 100% on multiple cores. Request-thread peak is the benchmark's in-flight worker count.

| Endpoint | Clients | RPS | p50 ms | p95 ms | p99 ms | CPU, one-core % | Log bytes | Peak request threads | Hikari active/pending/timeouts |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| lightweight | 1 | 13,218.2 | 0.030 | 0.148 | 0.476 | 304.7 | 554,000 | 1 | 0 / 0 / 0 |
| lightweight | 10 | 44,011.0 | 0.049 | 0.749 | 2.126 | 477.1 | 554,000 | 10 | 0 / 0 / 0 |
| lightweight | 25 | 41,426.1 | 0.047 | 2.562 | 2.994 | 407.4 | 554,000 | 25 | 0 / 0 / 0 |
| database | 1 | 1,719.3 | 0.458 | 1.164 | 2.762 | 194.2 | 548,000 | 1 | 1 / 1 / 0 |
| database | 10 | 5,702.0 | 1.372 | 4.219 | 8.140 | 263.3 | 548,007 | 10 | 3 / 10 / 0 |
| database | 25 | 4,702.2 | 4.079 | 16.364 | 25.261 | 236.7 | 548,265 | 25 | 3 / 24 / 0 |

Baseline queue peak, discarded events, and appender-failure count are not applicable because no queue or instrumentation exists. Hikari pending is workload contention in the intentionally constrained three-connection benchmark, not logging-induced database use; timeouts were zero.

## Release 4B1 implementation direction

- Route successful request INFO events to a dedicated bounded non-blocking asynchronous request appender.
- Route API WARN/ERROR and slow-request warnings to a separate synchronous reliable request file/logger.
- Keep security and audit on their existing independent reliable appenders; they must never enter the lossy request queue.
- Make queue capacity, discard threshold, non-blocking policy, caller-data policy, and maximum shutdown flush explicit in Logback configuration and production environment properties.
- Add aggregate queue-size/utilization/peak, discarded-event, written-byte, and appender-failure metrics with only an appender/category tag.
- Preserve rolling bounds, API fields, correlation IDs, and API responses. Do not change Hikari configuration.
