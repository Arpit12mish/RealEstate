# Hikari Pool Exhaustion Incident — 2026-07-30

## 1. Executive summary

**Confirmed:** the immediate failure was Hikari exhaustion, not a dead JVM or stopped Tomcat. **Strongly supported:** the underlying cause was the project-detail transaction chain borrowing up to three connections for one request: `ProjectServiceImpl.publicGet` opened the outer transaction, `ProjectMeterServiceImpl.publicGetMeterDetail` used `REQUIRES_NEW`, and its builder-credibility call used another `REQUIRES_NEW`. Five concurrent project details could retain five outer plus five meter connections, fill the ten-slot pool, and then wait for credibility connections that could not become available. A PostgreSQL 16 Testcontainers reproduction produced active=3, pending=3 and three acquisition timeouts with the same ownership pattern.

Both nested reads now use `REQUIRED`, so each project-detail request uses one transaction/connection at a time. Twilio and Instagram external I/O were also removed from database transactions. A repeated ten-client regression on a three-connection pool completed without timeouts; p95 was 186–204 ms in the final clean full-suite run and pending returned to zero. The production pool remains conservatively capped at 10.

## 2. Confirmed incident timeline

- **Confirmed:** 2026-07-30 13:15:29–13:16:29 IST: project, auth, OTP, guest-session and architect/designer requests stopped completing normally.
- **Confirmed:** around 13:15:59: `total=10, active=10, idle=0, waiting=15`; acquisition timed out near 30 seconds.
- **Confirmed:** waiting then rose to 27 and 32 while active remained 10 and idle remained zero.
- **Confirmed:** client/NGINX abandonment generated secondary broken pipes.
- **Confirmed:** restart recreated the pool and restored availability, while destroying the live connection-holder evidence.

## 3. Immediate failure mechanism

**Confirmed:** every Hikari slot was borrowed. New request transactions queued for a connection for the default 30-second timeout and then raised `SQLTransientConnectionException`, `JDBCConnectionException`, `CannotCreateTransactionException`, or `DataAccessResourceFailureException`. **Confirmed:** Tomcat continued accepting/processing work, which increased Hikari waiters. **Confirmed:** broken pipes were downstream effects of the backend stall.

## 4. Proven underlying root cause

**Strongly supported, high confidence:** one `GET /api/projects/{id}` could own connections recursively:

1. `ProjectServiceImpl.publicGet` starts a read-only transaction and acquires connection A.
2. `ProjectDetailComposerImpl.composePublic` calls `ProjectMeterServiceImpl.publicGetMeterDetail`.
3. The former `REQUIRES_NEW` suspends the outer transaction while A remains borrowed, then acquires B.
4. Meter composition calls `BuilderCredibilityServiceImpl.publicGetCredibilitySummary`.
5. Its former `REQUIRES_NEW` suspends both enclosing scopes and attempts to acquire C.

The incident named exactly five concurrent project IDs. Five requests can retain A+B (10 connections) and all wait for C. The deterministic pool-of-three reproduction proved the general mechanism: three outer transactions retained all three connections, the nested `REQUIRES_NEW` calls produced pending=3, and acquisition timed out. Git history places the nested meter propagation before the incident. The exact production thread/session snapshot was not preserved, so “Strongly supported” is more precise than claiming direct observation of each holder.

## 5. Contributing factors

- **Confirmed:** production had no explicit Hikari configuration, so it used maximum 10, minimum 10, and a 30-second acquisition timeout.
- **Confirmed:** OSIV was not explicitly disabled and therefore used Spring Boot’s enabled default, increasing the risk of late lazy access. It is now disabled.
- **Confirmed:** `TwilioOtpServiceImpl` had class-level `@Transactional`; Twilio SDK latency could retain a connection after repository access.
- **Confirmed:** Instagram scheduled/manual sync and thumbnail recache methods enclosed Meta/S3/download work in transactions.
- **Confirmed:** Tomcat’s default worker capacity was much larger than the pool, allowing many requests to wait simultaneously.
- **Hypothesis:** duplicate mobile fan-out and the shared t3.micro resource budget amplified the event; no mobile trace, CPU, memory or disk telemetry was supplied.

## 6. Hypotheses investigated and ruled out

| Finding | Conclusion |
|---|---|
| Manual `DataSource.getConnection`, unclosed `ResultSet`/`PreparedStatement`, Hibernate `doWork`, repository `Stream<T>` | **Not supported:** no leaking application pattern was found. |
| Permanent leak | **Not supported by reproduction:** active returned to zero after work. Production leak traces were unavailable. |
| Pool undersizing alone | **Not supported:** three connections served ten concurrent clients after transaction correction; maximum was not increased. |
| N+1 as primary trigger | **Not supported:** the controlled project-detail path is bounded at 20 statements and builder aggregation uses bulk `IN` queries. |
| PostgreSQL locks / idle-in-transaction | **Hypothesis:** production `pg_stat_activity`/`pg_locks` evidence was not captured. |
| Slow SQL / missing index as primary trigger | **Hypothesis:** no production statement timings or representative execution plans were supplied. |
| Infrastructure saturation | **Hypothesis:** no incident CPU, RAM, IO or PostgreSQL telemetry was supplied. |
| Lazy serialization as primary trigger | **Not supported in the regression:** explicit DTOs are built inside the service and the test succeeds with OSIV disabled. |

## 7. Files inspected

Configuration/deployment: `pom.xml`, all `application*.yml`, Logback configuration, security/Actuator configuration, datasource/JPA defaults, scheduler configuration, environment template, and repository deployment artifacts. **Confirmed:** no systemd unit, NGINX configuration, JVM flags, or PostgreSQL configuration is present in the repository.

Request flows: project controllers, `ProjectService`, `ProjectServiceImpl`, `ProjectDetailComposerImpl`, project/meter/builder-credibility entities, DTOs, mappers and repositories; auth, guest-session, OTP, token/session/rate-limit flows; architect/designer public flow; Instagram scheduler/sync; S3/Meta/Twilio clients; exception and request logging.

Searches covered `@Transactional`, `REQUIRES_NEW`, JDBC/EntityManager resource APIs, streams, fetch graphs, lazy/eager mappings, external clients, schedulers, async work, futures, sleeps and manual close operations.

Representative reconnaissance:

| File / method | Transaction before | Queries / external calls | Risk conclusion |
|---|---|---|---|
| `ProjectServiceImpl.publicGet` | read-only REQUIRED | project + media, then composer | **Confirmed:** owns outer connection through composition. |
| `ProjectMeterServiceImpl.publicGetMeterDetail` | read-only REQUIRES_NEW | meter/media/stage/compliance/pricing/amenity reads | **Confirmed:** retained second connection while outer was suspended. |
| `BuilderCredibilityServiceImpl.publicGetCredibilitySummary` | read-only REQUIRES_NEW | builder/projects plus bulk meter evidence | **Confirmed:** attempted a third connection in project detail. |
| `TwilioOtpServiceImpl.sendOtp/verifyOtp` | class-level REQUIRED | tracker reads/writes plus Twilio HTTP | **Confirmed:** external delay could retain a connection. |
| `InstagramReelSyncServiceImpl.sync*` | REQUIRED | Meta/download/S3 plus repository writes | **Confirmed:** external work was transaction-scoped. |
| guest-session flow | short repository transaction | session/user persistence; no provider call | **Not supported as initiating holder; affected as a waiter.** |
| architect/designer public detail | read-only database composition | repository/DTO mapping | **Not supported as initiating holder from available evidence; affected as a waiter.** |

## 8. Files changed

Incident-owned changes:

- transaction fixes: `ProjectMeterServiceImpl`, `BuilderCredibilityServiceImpl`, `TwilioOtpServiceImpl`, new `TwilioVerifyClient`/implementation, `InstagramReelSyncServiceImpl`, `OtpRequestTrackerRepository`;
- configuration/security/logging: `application-prod.yml`, `SecurityConfig`, `ApiRequestLoggingFilter`;
- verification: `NestedRequiresNewPoolStarvationReproductionTest`, `ProjectDetailQueryCountIntegrationTest`, `OtpVerifyFailureConcurrencyIntegrationTest`;
- operations: this report, the Hikari runbook, six diagnostic SQL scripts, and `scripts/load/hikari-10-device.js`.

**Confirmed:** the worktree contained extensive unrelated user changes before this incident task. They were preserved and are not attributed to this fix.

## 9. Database migrations added

**Confirmed:** none. Static predicate/index review did not justify a speculative index without representative execution plans. A fresh-database Flyway trial validated migration discovery but exposed a pre-existing failure at `V6__seed_dummy_business.sql` because `city_id=1` did not exist. Old applied migrations were not rewritten. Production/staging `flyway validate` against a sanitized schema copy is a mandatory rollout gate.

## 10. Configuration changes

| Area | Effective before | New production default |
|---|---|---|
| Hikari maximum / minimum | 10 / 10 (defaults) | 10 / 2 |
| connection / validation timeout | 30000 / 5000 ms | 5000 / 2000 ms |
| idle / max lifetime / keepalive | 600000 / 1800000 / 0 ms | unchanged, explicit |
| leak detection | 0 | 0 by default; environment-controlled diagnostic switch |
| auto-commit / isolation | true / driver default | unchanged |
| pool name / MBeans | `HikariPool-1` / false | `SfsHikariPool` / true |
| OSIV | true default | false |
| Hibernate batch fetch / JDBC batch | unset / 0 default | 25 / 25 |
| JPA query timeout / transaction timeout | none / none | 10000 ms / none |
| Hibernate statistics | false | false |
| Tomcat max/min threads | 200 / 10 defaults | 50 / 5 |
| accept count / max connections | 100 / 8192 defaults | 100 / 2048 |
| Tomcat connection/keepalive timeout | 20000 ms / inherits connection timeout | unchanged |

`ddl-auto` remains the non-embedded/Flyway-safe default (`none`); no runtime schema generation is enabled. Health/info/metrics are exposed through Actuator; metrics remain authenticated, health details are hidden, liveness excludes DB, and readiness includes DB. `/actuator/health/**` is reachable for probes.

**Unknown external values:** production PostgreSQL `max_connections`, buffers, timeouts, logging and `pg_stat_statements`; JVM heap/GC flags; systemd `MemoryMax`, `TasksMax`, restart policy; and NGINX timeouts. These require the production `SHOW` output and unit/config files listed in the rollout plan.

## 11. Code changes by component

- Project meter and builder credibility: both read methods join the existing transaction; duplicate stage loading in meter fallback mapping was removed.
- OTP: provider SDK calls moved behind a client interface and occur between short `TransactionTemplate` phases. Atomic PostgreSQL `INSERT ... ON CONFLICT DO NOTHING` plus pessimistic row locking prevents concurrent first-use tracker creation races.
- Instagram: Meta/download/S3 work is no longer enclosed by a service transaction; score recalculation explicitly saves modified rows.
- Security/Actuator: readiness and liveness probe paths are available while metric endpoints remain authenticated.
- Logging: structured request events add guest/anonymous/authenticated classification and response size when `Content-Length` is known; existing request ID, status, duration and exception fields remain.
- API compatibility: controllers and response DTO contracts were not changed.

## 12. Transaction-boundary findings

**Before:** project detail could retain A, B and request C through nested `REQUIRES_NEW`; Twilio and scheduled Instagram external calls ran inside transactions. **After:** project detail has one joined read transaction; Twilio performs a short locked pre-check, calls the provider with no active transaction/connection, then performs a short result update; Instagram repository operations use repository-local transactions around persistence only.

**Confirmed by test:** during a deliberately delayed Twilio provider call, Hikari active=0 and pending=0, and active returned to zero after completion. **Confirmed:** concurrent OTP tracker creation/failed attempts serialize without lost updates or duplicate-key failures.

## 13. Project-detail query analysis

The controlled PostgreSQL test invokes the real `ProjectService.publicGet`, meter composition and builder-credibility summary with OSIV disabled. Secondary connectivity/floor-plan/master-plan services are deterministic test doubles so the incident-sensitive core remains stable. It executes exactly 20 prepared statements on the seeded sparse project and is capped at 20.

Important observations: project/media are currently fetched twice across service composition; meter sections use fixed repository calls rather than per-row relationship traversal; builder credibility loads related projects and evidence with bulk queries; the former fallback summary repeated the construction-stage query and now reuses the loaded list. Local single-request duration was 97 ms in the final clean run; this is test-machine evidence, not a production SLO measurement.

## 14. PostgreSQL execution-plan findings

**Confirmed:** the important predicates are project primary key, project visibility flags, media `project_id` plus visibility/type/order, and meter/credibility child `project_id` joins. Existing schema/entity indexes cover primary/foreign-key lookup patterns relevant to the incident.

**Not available:** representative row counts and production/staging `EXPLAIN (ANALYZE, BUFFERS)` output. Plans on the one-row test fixture would prefer sequential scans and would not support a valid index decision. Therefore no index migration was added. Capture plans on a sanitized representative staging copy before any index change.

## 15. Connection-pool sizing rationale

The maximum remains 10. **Confirmed:** increasing it is unnecessary for the proven defect: ten concurrent clients completed repeatedly with a pool of three after fixing ownership. A t3.micro shares limited CPU/RAM with PostgreSQL and NGINX, so a larger pool could increase context switching and memory pressure. Minimum idle is reduced to 2 to avoid permanently reserving ten local PostgreSQL sessions. Any future maximum change requires production `max_connections`, memory, instance count, connection usage time and controlled-load throughput evidence.

## 16. Observability added

- standard Micrometer Hikari, HTTP, JVM, GC, process/system CPU and Tomcat metrics via authenticated `/actuator/metrics`;
- Hikari JMX registration and stable pool name;
- distinct liveness/readiness health groups with DB only in readiness;
- structured slow/error request fields including request ID, principal class, response size and exception;
- six read-only PostgreSQL diagnostic scripts for activity, blockers, long/idle transactions, statements and counts;
- alert thresholds and evidence-first recovery procedure in the runbook.

## 17. Tests added

- deterministic `REQUIRES_NEW` starvation reproduction against PostgreSQL 16;
- reflection assertions that both incident read methods use REQUIRED;
- three repeated ten-client runs with a pool of three, zero timeout requirement, drain assertions and p95 < 2000 ms;
- real project-detail core query-count/OSIV-off regression capped at 20;
- mocked two-second-class provider gate proving Twilio delay retains no connection;
- concurrent OTP first-use/failed-attempt serialization coverage.

## 18. Before/after load-test results

| Metric | Before | After | Improvement |
|---|---:|---:|---|
| Hikari maximum | 10 production / 3 reproduction | 10 production / 3 regression | no unsafe increase |
| Peak active | 10 production / 3 reproduction | 3 in constrained regression | bounded by pool; work completes |
| Peak pending | 32 production / 3 reproduction | not sampled continuously; 0 at every completion | queue drains |
| Hikari timeouts | present; ~30 s production / 3 deterministic | 0 across 30 client executions | eliminated in controlled workload |
| Project-detail query count | unbounded baseline not captured | 20 cap; 20 observed | regression guard added |
| Project-detail SQL/service time | production unavailable | 97 ms local sparse fixture | baseline established, not production-comparable |
| Ten-client p50 / p99 | unavailable | not captured by JUnit | run k6 in staging |
| Ten-client p95 | production requests reached ~30 s | 204, 193, 186 ms in final clean suite | below 2 s target |
| Error rate | incident-wide failures | 0/30 controlled executions | 100% controlled success |
| PostgreSQL active sessions / CPU / memory | unavailable | not captured by controlled JUnit | staging gate remains |

The k6 script targets the actual affected project-detail route with 10 VUs and reports HTTP rate/latency/status. It was not run because no authorized staging URL, representative IDs, or monitoring endpoint credentials were supplied; it was not pointed at production.

## 19. Remaining risks

- **Hypothesis:** production may also have slow plans, locks, idle transactions, or EC2 pressure; missing incident telemetry prevents ruling these out.
- The full response has fixed duplicate project/media work and optional secondary sections that warrant future measurement with representative data.
- OTP pre-check and provider result are intentionally separate transactions; provider ambiguity after network failure remains a business/idempotency concern.
- PostgreSQL/systemd/NGINX settings are external to the repository and unaudited.
- The historical Flyway chain cannot build a pristine database at V6 without prerequisite seed data; production validation must use a schema snapshot and this should be remediated separately without editing applied history.
- The worktree includes unrelated in-progress changes; deploy only the incident commit/patch set after review.

## 20. Production rollout plan

1. Preserve production logs and take/verify a database backup; export `flyway_schema_history`.
2. Collect `SHOW max_connections`, buffers/timeouts/log settings, `pg_stat_statements` status, systemd unit/environment key names (not values), JVM flags and NGINX timeouts.
3. Apply the incident commit to an isolated staging branch and run `flyway validate` against a sanitized production schema copy; no new migration should run.
4. Deploy staging, verify `/actuator/health/liveness` and `/actuator/health/readiness`, and authenticate to Hikari metrics.
5. Smoke launch, guest session, homepage, project list/details, OTP with test provider, and architect/designer detail.
6. Run `k6 run scripts/load/hikari-10-device.js` with staging `BASE_URL` and representative `PROJECT_IDS`; capture k6, Hikari, PostgreSQL, CPU and memory metrics.
7. Require zero timeouts/errors, pending recovery, stable readiness and acceptable p95 before approval.
8. Deploy during a controlled window using the existing artifact/systemd process; do not change PostgreSQL/pool maximum concurrently.
9. Monitor Hikari, HTTP, PostgreSQL sessions, JVM/Tomcat and host memory for 60 minutes; preserve logs.

## 21. Rollback plan

Rollback criteria: any Hikari timeout, sustained pending/active=max, readiness failure, material OTP/Instagram regression, elevated 5xx, or p95 above the agreed staging baseline. Preserve evidence first. Redeploy the previously known-good artifact/commit using the existing release mechanism, `sudo systemctl restart sfs.service`, verify health and mobile smoke flows, then confirm Hikari pending returns to zero. No database down migration is required because this change adds none. Do not terminate PostgreSQL sessions unless a confirmed blocker is reviewed by an operator.

## 22. Commands run

Key commands: branch/status/log/version inspection; repository-wide `rg`; `./mvnw test`; `./mvnw -DskipTests compile test-compile`; focused `-Dtest=...` Testcontainers suites; full test/build commands with local log-path override; Docker/Testcontainers PostgreSQL 16; Flyway fresh-schema diagnostic; and Surefire report inspection. No production command, deploy, load test, session termination, or secret-printing command was run.

## 23. Test results

- Baseline: 839 run, 814 passed, 21 local logging-context errors, 4 skipped, 0 assertion failures, 38.651 s. All 21 errors were inability to create `/var/log/sfs/app` in the restricted local environment.
- Focused incident suite: 6 tests, 0 failures/errors; reproduction, three repeated ten-client runs, query count and OTP tests passed.
- Migration diagnostic: 138 migrations discovered/validated by name/checksum before migration; pristine application failed at pre-existing V6 foreign-key seed ordering. No incident migration exists.
- Final clean full suite: 833 tests, 833 passed, 0 failures, 0 errors, 0 skipped; aggregate Surefire test time 20.565 seconds. PostgreSQL Testcontainers and the local log-path override were enabled.
- Final package: `./mvnw -DskipTests package` succeeded in 14.923 seconds; no formatter or static-analysis plugin is configured in `pom.xml`.

## 24. Commit hash

The immutable commit hash is reported in the final handoff (a commit cannot contain its own hash). Branch: `fix/hikari-pool-exhaustion`; starting point: `aa59db9dd7600391dc9bc4dea9c8bcacdc9ce92a`.

## 25. Final confidence level

**High** for the immediate failure and transaction amplification mechanism: production pool statistics, the exact affected request mix, code-level triple nesting, git timing, and deterministic PostgreSQL reproduction align. **Medium** that it was the only production contributor because live session/lock/query/host telemetry was not preserved. The fix is production-safe, conservative, API-compatible, and supported by repeated constrained-pool evidence; staging k6 plus production configuration capture remain rollout gates.
