SELECT pid,
       usename,
       now() - xact_start AS transaction_age,
       now() - state_change AS idle_for,
       left(query, 500) AS last_query
FROM pg_stat_activity
WHERE datname = current_database()
  AND state = 'idle in transaction'
ORDER BY xact_start;
