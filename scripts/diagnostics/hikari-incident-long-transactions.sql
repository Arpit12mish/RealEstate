-- Transactions older than one minute, including active and idle-in-transaction work.
SELECT pid,
       application_name,
       client_addr,
       usename,
       state,
       xact_start AS transaction_start,
       query_start,
       wait_event_type,
       wait_event,
       clock_timestamp() - xact_start AS transaction_age,
       clock_timestamp() - query_start AS query_age,
       left(regexp_replace(query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS query_text
FROM pg_stat_activity
WHERE datname = current_database()
  AND xact_start IS NOT NULL
  AND clock_timestamp() - xact_start > interval '1 minute'
ORDER BY xact_start;
