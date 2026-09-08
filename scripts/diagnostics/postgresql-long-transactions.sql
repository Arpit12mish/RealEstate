SELECT pid,
       usename,
       state,
       now() - xact_start AS transaction_age,
       wait_event_type,
       wait_event,
       left(query, 500) AS query
FROM pg_stat_activity
WHERE datname = current_database()
  AND xact_start IS NOT NULL
  AND now() - xact_start > interval '5 seconds'
ORDER BY xact_start;
