-- V161: User-behavior analytics event store (Phase 1).
--
-- Append-only, high-volume event table. Partitioned by month on occurred_at so that
-- retention (dropping old partitions) and index maintenance stay cheap as volume grows -
-- this codebase has no existing partitioning precedent, so partitioning starts here
-- rather than being retrofitted later onto a large table.
--
-- A DEFAULT partition is included as a safety net: if the partition-maintenance
-- scheduler (AnalyticsPartitionMaintenanceScheduler) ever falls behind on creating the
-- next month's partition, inserts still succeed into the default partition instead of
-- failing - analytics must never block or break app traffic.
--
-- Writes go through JdbcTemplate.batchUpdate (see AnalyticsEventJdbcRepository), never
-- JPA save() per row.

CREATE TABLE analytics_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    schema_version SMALLINT NOT NULL DEFAULT 1,
    event_name VARCHAR(60) NOT NULL,
    anonymous_id UUID NOT NULL,
    user_id BIGINT,
    session_id UUID NOT NULL,
    screen VARCHAR(80),
    entity_type VARCHAR(40),
    entity_id BIGINT,
    search_id UUID,
    result_position INTEGER,
    numeric_value DOUBLE PRECISION,
    source_platform VARCHAR(10) NOT NULL,
    app_version VARCHAR(30),
    properties JSONB,
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

-- Composite indexes propagate automatically to every partition (present and future)
-- created via "PARTITION OF" - no need to repeat them per partition.
CREATE INDEX idx_analytics_event_name_occurred ON analytics_event (event_name, occurred_at);
CREATE INDEX idx_analytics_event_user_occurred ON analytics_event (user_id, occurred_at) WHERE user_id IS NOT NULL;
CREATE INDEX idx_analytics_event_session ON analytics_event (session_id);
CREATE INDEX idx_analytics_event_search ON analytics_event (search_id) WHERE search_id IS NOT NULL;
CREATE INDEX idx_analytics_event_entity ON analytics_event (entity_type, entity_id, occurred_at) WHERE entity_type IS NOT NULL;
-- BRIN is near-free on an append-only, time-ordered table and speeds up the sequential
-- range scans the nightly aggregation job runs over a whole day's partition.
CREATE INDEX idx_analytics_event_occurred_brin ON analytics_event USING BRIN (occurred_at);

-- Bootstrap the current month plus three months ahead so the app has headroom even if
-- the maintenance scheduler's first run is delayed after deploy.
DO $$
DECLARE
    start_month date := date_trunc('month', now())::date;
    i integer;
    partition_start date;
    partition_end date;
    partition_name text;
BEGIN
    FOR i IN 0..3 LOOP
        partition_start := (start_month + (i || ' months')::interval)::date;
        partition_end := (start_month + ((i + 1) || ' months')::interval)::date;
        partition_name := 'analytics_event_' || to_char(partition_start, 'YYYY_MM');
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF analytics_event FOR VALUES FROM (%L) TO (%L)',
            partition_name, partition_start, partition_end
        );
    END LOOP;
END $$;

CREATE TABLE IF NOT EXISTS analytics_event_default PARTITION OF analytics_event DEFAULT;

-- Guest -> user attribution, mirroring the existing append-only guest_identity_link
-- pattern (V148): written once at login, joined at query time by aggregation jobs.
-- Historical raw analytics_event rows are never rewritten.
CREATE TABLE analytics_identity_link (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    anonymous_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_identity_link_anon ON analytics_identity_link (anonymous_id);
CREATE INDEX idx_analytics_identity_link_user ON analytics_identity_link (user_id);

-- Dashboard reads hit these derived aggregates, never the raw partitioned table -
-- see AnalyticsAggregationScheduler. Recomputed idempotently per day via upsert.
CREATE TABLE analytics_daily_search (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_date DATE NOT NULL,
    normalized_query VARCHAR(500) NOT NULL,
    search_count BIGINT NOT NULL DEFAULT 0,
    unique_user_count BIGINT NOT NULL DEFAULT 0,
    zero_result_count BIGINT NOT NULL DEFAULT 0,
    avg_result_count DOUBLE PRECISION,
    click_count BIGINT NOT NULL DEFAULT 0,
    avg_click_position DOUBLE PRECISION,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_analytics_daily_search UNIQUE (metric_date, normalized_query)
);
CREATE INDEX idx_analytics_daily_search_date ON analytics_daily_search (metric_date);

CREATE TABLE analytics_daily_entity_metrics (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    metric_date DATE NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,
    unique_user_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_analytics_daily_entity_metrics UNIQUE (metric_date, entity_type, entity_id)
);
CREATE INDEX idx_analytics_daily_entity_metrics_date ON analytics_daily_entity_metrics (metric_date);
