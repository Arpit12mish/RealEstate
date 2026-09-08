# Release 5A-P: production-aligned local staging

## Scope and decision

Release 5A capacity testing is blocked because no authorized AWS staging environment exists. This package provides a local correctness environment only; it does not deploy externally, contact production, change mobile clients, add response caching, or establish EC2 capacity.

The selected profile is standalone `local-staging` (Option A). Reusing `prod,local-staging` is unsafe because the production profile owns the live Twilio implementation and initializes the SDK eagerly. The standalone profile repeats only the runtime values needed to make important limits explicit and auditable.

## Reconnaissance summary

| Concern | Existing behavior | Local-staging action | Risk controlled |
|---|---|---|---|
| Datasource | Spring/Hikari PostgreSQL; production pool max 10 | Canonical `SPRING_DATASOURCE_*`; max 10/min 2; PostgreSQL 16 | Pool/config drift |
| Persistence | Production OSIV off, Flyway on, schema validation | Same settings; clean disabled | Lazy-loading and schema drift |
| Fresh migrations | Historical chain has missing/invalid base migrations and entity/DDL drift | Profile-only Flyway callback supplies disposable prerequisites and type alignment | Fresh bootstrap failure without rewriting applied migrations |
| Tomcat | Production 50/5 threads, queue 100, 2048 connections | Same effective defaults | Unrealistic servlet concurrency |
| OTP | Prod Twilio bean initializes eagerly; fake provider existed for local profiles | Explicit-property local-staging fake provider; prod isolation test | Real SMS or fixed OTP in prod |
| Elasticsearch | Client/config existed even when search disabled | Conditional real stack plus disabled gateway | Startup connection/client creation |
| Cloud/providers | S3/Google/Meta/schedulers configurable | Invalid/blank destinations and explicit disables | External writes/calls |
| Security/Actuator | Existing chain permits health and protects metrics | Preserved unchanged | Metrics exposure/security regression |
| Rate limiting | Release 4B2 custom exact-proxy resolver | Fixed NGINX IP only; Spring forwarding disabled | Spoofed/double-processed XFF |
| Logging | Bounded async request queue and rolling files | Queue 2048; writable non-root bind mount | Request-thread blocking/permission failure |
| Seed data | Dashboard seeder can mutate data when enabled | Disabled; no new broad fixture seeder | Accidental accounts/production activation |

## Architecture and configuration

The only host-facing listener is NGINX on `127.0.0.1:8088`. NGINX proxies over a dedicated bridge to an unpublished non-root Spring Boot container, which reaches an unpublished PostgreSQL 16 container. PostgreSQL persists to a project-scoped named volume. A separate Compose override can bind PostgreSQL diagnostics to loopback.

The backend image is built with Java 17 and the Maven wrapper, then copied as the deterministic `sfs-0.0.1-SNAPSHOT.jar` into a Java 17 JRE image. It runs as UID/GID 10001, uses exec-form `java` for SIGTERM delivery, and defaults to a 256–512 MiB G1 heap inside a 1 GiB container. These JVM flags are local correctness defaults, not claimed production settings.

Production-aligned values include Hikari max 10/min 2, 5 s checkout timeout, 2 s validation timeout, OSIV false, Hibernate batch sizes 25, query timeout 10 s, Tomcat max 50/min 5, accept count 100, and max connections 2048. Mobile/dashboard identity cache bounds and TTLs, the 2048-event request-log queue, rate-limit import, probes, and Swagger-off behavior are preserved.

The local profile constructs Hikari from a fully bound `HikariConfig`. This is a narrow compatibility path for Spring Boot 4/Hikari 7: direct late binding to a started `HikariDataSource` otherwise attempts to mutate a sealed pool. It is profile-isolated and does not alter production datasource wiring.

## Provider and data safety

Elasticsearch clients, its index initializer, and its real gateway now require `sfs.search.enabled=true`; the disabled gateway retains the database fallback contract. Local staging disables search and uses invalid non-routable service configuration.

Fake OTP requires both a local profile and `sfs.local-staging.fake-otp.enabled=true`; profile tests prove `prod` cannot instantiate it. The implementation never logs OTP or phone values. Google Places, Meta sync, refresh-token cleanup, dashboard seed, and review OTP are disabled. AWS metadata/static credentials are disabled and no real bucket or public base URL is supplied.

No new generalized fixture generator was added. Existing Flyway data is enough for a public API smoke check. A profile-only callback supplies the exact IDs assumed by historical sample migrations and repairs fresh-bootstrap prerequisites that the versioned chain never supplied. It is isolated by a location loaded only in `application-local-staging.yml`.

The clean PostgreSQL 16 bootstrap exposed migration debt that is masked by long-lived databases: `V12_loginHistory.sql` is not a valid Flyway versioned filename; `users`, `builder`, `company`, `project`, several mapped child tables, and a few columns are referenced before any versioned creation; V40 alters a legacy table that is never created; sample cost rules assume company IDs 1/3/4/5; and several numeric columns disagree with `Double` entity mappings. Existing applied migrations remain byte-for-byte unchanged. The callback reconstructs only these disposable local prerequisites so Flyway can complete and Hibernate `validate` can remain enabled. Production should eventually receive forward-only migrations that reconcile each confirmed drift; the local callback must never be added to production Flyway locations.

## NGINX and trusted proxy behavior

NGINX deliberately overwrites, rather than appends, incoming `X-Forwarded-For` because this topology contains one trusted proxy. It passes request ID, real peer IP, scheme, and host; uses upstream keepalive; and logs JSON method, path (without query), status, latency, upstream latency, and bytes. It never logs authorization headers.

The backend uses `server.forward-headers-strategy=none`. Release 4B2's `ClientIpResolver` alone evaluates forwarded data and trusts only the exact configurable NGINX container IP. The subnet must be checked for collision before startup. Direct host access to the backend is prevented by the absence of a published port.

## Operations and diagnostics

See [`infra/local-staging/README.md`](../../infra/local-staging/README.md) for commands. Helper scripts validate Compose, require an ignored `.env`, reject production values/profile selection, wait for readiness, verify protected metrics, and refuse broad deletion. Reset removes only the exactly named local PostgreSQL volume after interactive confirmation.

Diagnostics capture readiness, Hikari, Tomcat, JVM/GC, CPU, rate-limit cache, identity cache, and async logging metrics. SQL reports cover active sessions, blocking locks, long transactions, and idle-in-transaction sessions. Local diagnostics intentionally use the Flyway/application owner rather than creating a role before tables exist and falsely implying future-object grants.

k6 scripts have no default URL, require `TARGET_ENV=local|staging`, reject known production hosts/IPs, and reject non-local destinations in local mode. Only smoke and bounded rate-limit security verification are included; endpoint capacity, mixed workload, spike, and soak scenarios remain out of scope.

## Validation record

Final exact-index validation on 2026-07-31:

- Maven tests: 854 run, 0 failures, 0 errors, 0 skipped; PostgreSQL 16 Testcontainers executed.
- Maven package: `BUILD SUCCESS`.
- Compose config/image build: valid; exact staged image built from 1,218 source files and runs as UID/GID 10001.
- PostgreSQL/Flyway/Hibernate startup: PostgreSQL 16.14; 133 successful versioned migrations through V134, 0 failed; Hibernate validation passed from an empty volume.
- NGINX/API/runtime smoke: NGINX configuration valid; readiness, liveness, public API, actuator protection, and unpublished backend/database ports passed.
- Runtime metrics: Hikari active/pending/timeouts returned 0/0/0 with max 10; Tomcat current/busy/max instrumentation was available; the diagnostic collector produced 28 files with no unavailable metric.
- Rate-limit/invalid-bearer smoke: 10 invalid requests returned 401 followed by 100 bounded 429 responses; all 429 responses passed `Retry-After >= 1`; varying spoofed forwarded addresses still produced 10/2 (401/429), with final `Retry-After: 43`.
- Logging/provider safety: 100 routine 429 records used `sfs-api.log`, none used the reliable request log; test phone, OTP, invalid bearer, and external-provider host patterns had zero log matches.
- k6: production target guard failed closed; script inspection passed; low-volume smoke completed 6 requests at 0% failure with p95 60.66 ms; bounded invalid-bearer checks passed 220/220.

## Remaining AWS Release 5A work

An authorized production-like staging environment is still required for Android/iOS journeys, real request fan-out, provider integration smoke, endpoint and mixed workloads at staged RPS, controlled spike, one-hour soak, PostgreSQL/host/credit observation, bottleneck identification, and an operating limit with at least 30% headroom. Local Docker results must not be extrapolated into production capacity.
