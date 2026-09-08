-- Requires pg_stat_statements. If the extension is unavailable, this SELECT fails safely.
SELECT calls,
       round(total_exec_time::numeric, 2) AS total_exec_ms,
       round(mean_exec_time::numeric, 2) AS mean_exec_ms,
       round(max_exec_time::numeric, 2) AS max_exec_ms,
       rows,
       shared_blks_hit,
       shared_blks_read,
       temp_blks_written,
       left(regexp_replace(query, E'[\\n\\r\\t]+', ' ', 'g'), 500) AS query_text
FROM pg_stat_statements
WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY total_exec_time DESC
LIMIT 50;
