SELECT blocked.pid AS blocked_pid,
       blocker.pid AS blocking_pid,
       now() - blocked.query_start AS blocked_for,
       left(blocked.query, 500) AS blocked_query,
       left(blocker.query, 500) AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_stat_activity blocker
  ON blocker.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE blocked.datname = current_database()
ORDER BY blocked.query_start;
