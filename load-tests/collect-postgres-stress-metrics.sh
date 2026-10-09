#!/usr/bin/env bash
set -euo pipefail

INTERVAL_SECONDS="${INTERVAL_SECONDS:-0.5}"
DURATION_SECONDS="${DURATION_SECONDS:-390}"
PGDATABASE="${PGDATABASE:-sfs_db}"
PGUSER="${PGUSER:-sfs_user}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUTPUT_PREFIX="${OUTPUT_PREFIX:-postgres-stress-metrics-${STAMP}}"
CSV_PATH="${OUTPUT_PREFIX}.csv"
SUMMARY_PATH="${OUTPUT_PREFIX}-summary.json"

printf '%s\n' 'timestamp_utc,total_sessions,active_sessions,idle_sessions,idle_in_transaction,lock_waits,longest_transaction_seconds,longest_query_seconds,deadlocks,xact_commit,xact_rollback,temp_files,temp_bytes,blks_read,blks_hit' > "$CSV_PATH"

START_EPOCH="$(date +%s)"
while (( $(date +%s) - START_EPOCH < DURATION_SECONDS )); do
  psql -X -qAt -F',' -v ON_ERROR_STOP=1 \
    -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "
WITH activity AS (
  SELECT
    count(*) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid()) AS total_sessions,
    count(*) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND state = 'active') AS active_sessions,
    count(*) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND state = 'idle') AS idle_sessions,
    count(*) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND state = 'idle in transaction') AS idle_in_transaction,
    count(*) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND wait_event_type = 'Lock') AS lock_waits,
    coalesce(max(extract(epoch FROM clock_timestamp() - xact_start)) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND xact_start IS NOT NULL), 0) AS longest_transaction_seconds,
    coalesce(max(extract(epoch FROM clock_timestamp() - query_start)) FILTER (WHERE usename = current_user AND pid <> pg_backend_pid() AND state = 'active'), 0) AS longest_query_seconds
  FROM pg_stat_activity
), database_stats AS (
  SELECT deadlocks, xact_commit, xact_rollback, temp_files, temp_bytes, blks_read, blks_hit
  FROM pg_stat_database WHERE datname = current_database()
)
SELECT to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'),
       total_sessions, active_sessions, idle_sessions, idle_in_transaction, lock_waits,
       round(longest_transaction_seconds::numeric, 6), round(longest_query_seconds::numeric, 6),
       deadlocks, xact_commit, xact_rollback, temp_files, temp_bytes, blks_read, blks_hit
FROM activity CROSS JOIN database_stats;" >> "$CSV_PATH"
  sleep "$INTERVAL_SECONDS"
done

awk -F',' '
NR == 2 { d0=$9; c0=$10; r0=$11; tf0=$12; tb0=$13; br0=$14; bh0=$15 }
NR > 1 {
  n++; if ($2>ts)ts=$2; if ($3>a)a=$3; if ($4>i)i=$4; if ($5>iit)iit=$5;
  if ($6>lw)lw=$6; if ($7>tx)tx=$7; if ($8>q)q=$8;
  d1=$9; c1=$10; r1=$11; tf1=$12; tb1=$13; br1=$14; bh1=$15
}
END {
  printf "{\n  \"samples\": %d,\n  \"maximumTotalSessions\": %d,\n  \"maximumActiveSessions\": %d,\n  \"maximumIdleSessions\": %d,\n  \"maximumIdleInTransaction\": %d,\n  \"maximumLockWaits\": %d,\n  \"longestTransactionSeconds\": %.6f,\n  \"longestQuerySeconds\": %.6f,\n  \"deadlocksDelta\": %d,\n  \"xactCommitDelta\": %d,\n  \"xactRollbackDelta\": %d,\n  \"tempFilesDelta\": %d,\n  \"tempBytesDelta\": %d,\n  \"blocksReadDelta\": %d,\n  \"blocksHitDelta\": %d\n}\n", n,ts,a,i,iit,lw,tx,q,d1-d0,c1-c0,r1-r0,tf1-tf0,tb1-tb0,br1-br0,bh1-bh0
}' "$CSV_PATH" > "$SUMMARY_PATH"

printf 'PostgreSQL CSV: %s\n' "$CSV_PATH"
printf 'PostgreSQL summary: %s\n' "$SUMMARY_PATH"
