-- Connection totals by application/user/state plus configured database capacity.
SELECT application_name,
       usename,
       state,
       count(*) AS connections
FROM pg_stat_activity
WHERE datname = current_database()
GROUP BY application_name, usename, state
ORDER BY connections DESC, application_name, usename, state;

SELECT current_setting('max_connections')::integer AS max_connections,
       current_setting('superuser_reserved_connections')::integer AS superuser_reserved_connections,
       count(*) FILTER (WHERE datname = current_database()) AS current_database_connections,
       count(*) AS cluster_connections
FROM pg_stat_activity;
