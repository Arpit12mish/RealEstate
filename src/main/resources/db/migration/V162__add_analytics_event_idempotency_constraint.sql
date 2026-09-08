-- V162: Idempotency for analytics_event under client-side retry.
--
-- Context: the mobile HTTP client (services/httpClient.ts, NETWORK_RETRY_COUNT=1)
-- automatically retries a request exactly once on a pure network failure - including
-- the analytics batch endpoint. If the original request actually reached the server
-- and was persisted, but the response was lost (timeout/connection drop), the retry
-- resends the identical request body, so the same eventId can arrive twice. Every
-- current aggregation query uses count(*), not count(DISTINCT event_id), so a raw
-- duplicate row would double-count in search_count/zero_result_count/click_count/
-- view_count. Chosen fix: prevent the duplicate row from ever being written, at the
-- single point closest to the write (AnalyticsEventJdbcRepository), rather than
-- scattering "remember to dedupe" logic across every current and future aggregation/
-- drill-down query that reads analytics_event.
--
-- analytics_event is partitioned by RANGE(occurred_at); PostgreSQL requires any
-- unique constraint on a partitioned table to include the partition key column, so
-- UNIQUE(event_id) alone is not permitted - UNIQUE(event_id, occurred_at) is. This is
-- sufficient here because a genuine client retry resends the exact same event object
-- (same eventId AND same client-captured occurredAt, generated once in
-- AnalyticsClient.track() and never regenerated on retry) - so the retried delivery
-- always collides on this exact pair. A UNIQUE INDEX created directly on a partitioned
-- table propagates automatically to every existing and future partition (same
-- mechanism already relied on for V161's other indexes), so this single statement
-- covers all partitions, including analytics_event_default.
--
-- Defensive dedupe first: if any (event_id, occurred_at) pair already has more than
-- one row (there should be none - every eventId sent during Phase 1 testing was
-- freshly generated - but this migration must not fail if that assumption is wrong,
-- e.g. in an environment that already saw a real duplicate delivery before this fix
-- shipped), keep the lowest id and delete the rest before the index is created.
DELETE FROM analytics_event a
USING analytics_event b
WHERE a.event_id = b.event_id
  AND a.occurred_at = b.occurred_at
  AND a.id > b.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_analytics_event_id_occurred
    ON analytics_event (event_id, occurred_at);
