# Hikari Pool Exhaustion Runbook

## Trigger

Treat any of these as an incident: `hikaricp.connections.pending > 0` for 15 seconds, active equals max for 15 seconds, a positive Hikari timeout delta, readiness failure, or a sustained API 5xx/latency spike.

Do not restart first unless availability requires it. A restart destroys the connection, lock, thread, and transaction evidence needed to identify the holder.

## 1. Preserve application evidence

```bash
sudo journalctl -u sfs.service --since '-15 minutes' --no-pager > /tmp/sfs-hikari-journal.txt
sudo jcmd "$(pgrep -f 'sfs-.*\.jar' | head -n 1)" Thread.print -l > /tmp/sfs-hikari-threads.txt
sudo jcmd "$(pgrep -f 'sfs-.*\.jar' | head -n 1)" GC.heap_info > /tmp/sfs-hikari-heap.txt
```

Record the exact incident time in IST, instance ID, deployed commit, active profile, pool maximum, and whether a scheduler ran. Never include environment files, tokens, or credentials in the evidence bundle.

## 2. Check Hikari and HTTP metrics

The metrics endpoint is authenticated by Spring Security; only health is public.

```bash
curl -fsS -H "Authorization: Bearer $SFS_DIAGNOSTIC_TOKEN" http://127.0.0.1:8080/actuator/metrics/hikaricp.connections.active
curl -fsS -H "Authorization: Bearer $SFS_DIAGNOSTIC_TOKEN" http://127.0.0.1:8080/actuator/metrics/hikaricp.connections.idle
curl -fsS -H "Authorization: Bearer $SFS_DIAGNOSTIC_TOKEN" http://127.0.0.1:8080/actuator/metrics/hikaricp.connections.pending
curl -fsS -H "Authorization: Bearer $SFS_DIAGNOSTIC_TOKEN" http://127.0.0.1:8080/actuator/metrics/hikaricp.connections.timeout
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
curl -fsS http://127.0.0.1:8080/actuator/health/liveness
```

Also inspect `http.server.requests`, `tomcat.threads.busy`, `jvm.memory.used`, `jvm.gc.pause`, `process.cpu.usage`, and `system.cpu.usage`. Use `scripts/sfs-logs` or the JSON API log to locate `slow_api` and `api_request_failed` events by `requestId`.

## 3. Capture PostgreSQL state

Run every script against the production database with a read-only diagnostic role where possible:

```bash
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-connection-counts.sql
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-active-sessions.sql
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-blocking-locks.sql
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-long-transactions.sql
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-idle-in-transaction.sql
psql "$SFS_DIAGNOSTIC_DATABASE_URL" -X -f scripts/diagnostics/hikari-incident-slow-queries.sql
```

These scripts are read-only and never terminate sessions.

## 4. PostgreSQL diagnostic settings

Recommended permanent guardrails, after staging validation:

```conf
log_lock_waits = on
deadlock_timeout = '1s'
idle_in_transaction_session_timeout = '60s'
statement_timeout = '15s'
```

Temporary investigation setting when log volume is understood:

```conf
log_min_duration_statement = '1000ms'
```

Enable `pg_stat_statements` permanently if operational policy allows it; it requires `shared_preload_libraries` and a controlled restart. Prefer role/database-scoped timeouts when other workloads share the cluster.

## 5. Recovery

1. Stop or disable only a confirmed offending scheduled/manual job if it is still generating work.
2. Reduce incoming traffic at NGINX or application rate limits if pending connections continue rising.
3. Do not terminate PostgreSQL sessions until the blocking PID/query and business impact are confirmed.
4. If availability requires a service restart, preserve all evidence above first, then use `sudo systemctl restart sfs.service` and verify readiness, pending connections, error rate, and mobile smoke flows.
5. Escalate immediately if pending does not return to zero, active remains at max, or readiness does not recover within five minutes.

## Alerts

- Critical: pending > 0 for 15s; active == max for 15s; Hikari timeout delta > 0; readiness down; repeated 5xx; PostgreSQL connections near the configured limit.
- Warning: pool utilization > 80%; API p95 above 1.5s; long or idle transaction; Tomcat busy threads > 80%; JVM memory pressure; EC2 memory pressure after CloudWatch Agent installation.
