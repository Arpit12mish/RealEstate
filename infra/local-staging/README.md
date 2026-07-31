# SFS local staging

This environment runs the SFS backend in a production-aligned, local-only topology:

```text
Mac (127.0.0.1:8088) -> NGINX -> Spring Boot -> PostgreSQL 16
```

It is intended for startup, migration, proxy, security, and low-volume integration checks. It is not an EC2 capacity benchmark and must never be pointed at production.

## Prerequisites

- Docker Desktop with Compose v2
- `curl`, `rg`, and Java 17 for host-side checks
- Optional: `k6`, or the `grafana/k6` image

Check that `LOCAL_STAGING_SUBNET` in `.env` does not overlap another Docker or private network before startup.

## Configure and run

```bash
cp infra/local-staging/.env.example infra/local-staging/.env
infra/local-staging/up.sh
infra/local-staging/status.sh
infra/local-staging/smoke.sh
```

`.env` is ignored. Its values are deliberately local and unsafe for real deployments. Use `SFS_BUILD_COMMIT=$(git rev-parse HEAD)` when building a clean committed tree so the image revision label is traceable. The scripts refuse production hostnames/IPs and require the active profile to be exactly `local-staging`.

Stop without deleting data:

```bash
infra/local-staging/down.sh
```

Delete only this environment's PostgreSQL volume (interactive `RESET` confirmation required):

```bash
infra/local-staging/reset.sh
```

The backend and PostgreSQL ports are not published. The optional diagnostics override exposes PostgreSQL on loopback only:

```bash
docker compose --env-file infra/local-staging/.env \
  -f infra/local-staging/docker-compose.yml \
  -f infra/local-staging/docker-compose.diagnostics.yml up -d postgres
```

## Safety and isolation

- Profile strategy is standalone `local-staging`; activating `prod` would initialize the production-only Twilio client.
- Fake OTP is available only under a local profile and only when `SFS_LOCAL_STAGING_FAKE_OTP_ENABLED=true`. The local test value is never logged. Production cannot activate this bean.
- Elasticsearch, Google Places, Meta sync, refresh-token cleanup, dashboard seeding, and review fixed OTP are disabled.
- AWS metadata access and static credentials are disabled; the S3 destination is an invalid local placeholder.
- Hikari remains capped at 10; OSIV is off; Hibernate validates the migrated schema.
- Actuator health is public per the existing security contract. Metrics retain the existing authentication requirement.
- NGINX overwrites client-supplied `X-Forwarded-For` with its observed peer address. Spring's forwarding strategy is `none`, leaving Release 4B2's resolver as the sole trust authority. Only the exact fixed NGINX container IP is trusted.
- NGINX logs `$uri`, not `$request_uri`, so query values and authorization headers are absent.

## Flyway and database checks

The fresh-schema migration chain has a historical prerequisite: V6 references `city.id=1`, but no earlier migration inserts it. `db/local-staging/beforeEachMigrate.sql` creates only that synthetic prerequisite after V1 and only because this profile adds the callback location. Existing migrations are unchanged and production never loads the callback.

Verify migration history and PostgreSQL version:

```bash
docker compose --env-file infra/local-staging/.env \
  -f infra/local-staging/docker-compose.yml exec -T postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select version(); select installed_rank,version,success from flyway_schema_history order by installed_rank;"'
```

Local diagnostics use the application-owned database role. There is no misleading pre-migration read-only role whose grants omit future Flyway objects.

## Health, logs, and runtime checks

```bash
curl --fail http://127.0.0.1:8088/local-health
curl --fail http://127.0.0.1:8088/actuator/health/readiness
docker compose --env-file infra/local-staging/.env \
  -f infra/local-staging/docker-compose.yml logs backend nginx
```

Application rolling logs are written to `infra/local-staging/runtime/logs` by UID/GID 10001. Container stdout contains NGINX JSON access logs. No secrets should be printed.

To capture protected metrics, supply a staging-only access token without writing it to a command history or report:

```bash
BASE_URL=http://127.0.0.1:8088 ACTUATOR_TOKEN="$TOKEN" \
  scripts/diagnostics/capture-actuator-baseline.sh
```

Confirm `hikaricp.connections.max=10`, pending/timeouts remain zero, and active returns to zero after work. Micrometer supplies these metrics; local staging deliberately does not force Hikari JMX registration. A profile-only datasource factory binds the canonical `spring.datasource.hikari.*` settings to `HikariConfig` before constructing the Hikari 7 pool, avoiding late mutation after the pool is sealed. Reports go under ignored `scripts/diagnostics/output/`.

## Proxy and rate-limit checks

The backend port is deliberately unpublished, so host traffic must traverse NGINX. A spoofed client `X-Forwarded-For` is overwritten; NGINX's exact bridge IP is the configured trusted peer. Existing resolver unit/integration tests cover malformed and untrusted chains.

Low-volume checks only:

```bash
TARGET_ENV=local BASE_URL=http://127.0.0.1:8088 k6 inspect scripts/load/smoke.js
TARGET_ENV=local BASE_URL=http://127.0.0.1:8088 k6 run scripts/load/smoke.js
TARGET_ENV=local BASE_URL=http://127.0.0.1:8088 k6 run scripts/load/rate-limit-security.js
```

The security smoke uses invalid, non-secret local bearer values, verifies they are never accepted, and requires any 429 `Retry-After` to be at least one. It is not a capacity test. The scripts require `TARGET_ENV` and `BASE_URL`, reject production targets, and permit the `nginx` hostname only for a k6 container attached to this private Compose network.

## Troubleshooting

- `Missing .env`: copy `.env.example`; do not commit `.env`.
- Network overlap: choose a free RFC1918 `/24` and matching NGINX address in `.env`.
- Migration failure: inspect backend logs and `flyway_schema_history`; do not edit an applied migration.
- Unhealthy backend: inspect `/actuator/health/readiness`, PostgreSQL health, file ownership under `runtime/logs`, and container logs.
- Metrics return 401/403: expected without a valid authenticated user.
- Provider connection attempts: stop the environment and verify the disable flags/profile before proceeding.

AWS Release 5A still requires an authorized production-like environment, real mobile smoke journeys, production-like provider wiring, request-fan-out capture, staged RPS tests, spike/soak tests, host/credit metrics, and a conservative operating limit with headroom.
