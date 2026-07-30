-- Read-only snapshot of non-idle PostgreSQL sessions during a Hikari incident.
SELECT pid,
       application_name,
       client_addr,
       usename,
       state,
       xact_start AS transaction_start,
       query_start,
       wait_event_type,
       wait_event,
       clock_timestamp() - query_start AS query_age,
       clock_timestamp() - xact_start AS transaction_age,
       left(regexp_replace(query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS query_text
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid()
  AND state <> 'idle'
ORDER BY query_start NULLS LAST;
