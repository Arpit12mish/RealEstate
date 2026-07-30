-- Read-only blocking tree. This script never cancels or terminates a backend.
SELECT blocked.pid AS blocked_pid,
       blocker.pid AS blocking_pid,
       blocked.application_name AS blocked_application,
       blocker.application_name AS blocking_application,
       blocked.usename AS blocked_user,
       blocked.wait_event_type,
       blocked.wait_event,
       clock_timestamp() - blocked.query_start AS blocked_query_age,
       clock_timestamp() - blocker.xact_start AS blocking_transaction_age,
       left(regexp_replace(blocked.query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS blocked_query,
       left(regexp_replace(blocker.query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS blocking_query
FROM pg_stat_activity blocked
CROSS JOIN LATERAL unnest(pg_blocking_pids(blocked.pid)) AS blocking_pid
JOIN pg_stat_activity blocker ON blocker.pid = blocking_pid
WHERE blocked.datname = current_database()
ORDER BY blocked.query_start;
