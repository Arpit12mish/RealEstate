-- Idle transactions retain locks/snapshots and may retain application connections.
SELECT pid,
       application_name,
       client_addr,
       usename,
       state,
       xact_start AS transaction_start,
       state_change,
       wait_event_type,
       wait_event,
       clock_timestamp() - xact_start AS transaction_age,
       clock_timestamp() - state_change AS idle_age,
       left(regexp_replace(query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS last_query
FROM pg_stat_activity
WHERE datname = current_database()
  AND state IN ('idle in transaction', 'idle in transaction (aborted)')
ORDER BY xact_start;
