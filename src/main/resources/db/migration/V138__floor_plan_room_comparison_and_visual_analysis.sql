-- Floor Plan Insights redesign: per-room space comparison + visual-analysis media/tags.
-- All new columns/tables are additive; existing rooms[]/insights[] responses are unaffected.

-- ─── project_floor_plan_room_dimension: room-level comparison fields ─────────
-- Comparison data lives directly on the room row (not joined through
-- project_floor_plan_insight) because insight_type coverage does not span
-- every FloorPlanRoomType and there is no FK between the two tables.

ALTER TABLE project_floor_plan_room_dimension
    ADD COLUMN IF NOT EXISTS average_area_sqft        DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS comparison_context_label VARCHAR(120),
    ADD COLUMN IF NOT EXISTS difference_percent       DECIMAL(6, 2),
    ADD COLUMN IF NOT EXISTS comparison_summary       TEXT,
    ADD COLUMN IF NOT EXISTS comparison_verified      BOOLEAN NOT NULL DEFAULT FALSE;

-- ─── project_floor_plan_visual_analysis: one row per floor plan ─────────────

CREATE TABLE IF NOT EXISTS project_floor_plan_visual_analysis (
    id             BIGSERIAL    PRIMARY KEY,
    floor_plan_id  BIGINT       NOT NULL REFERENCES project_floor_plan (id),
    title          VARCHAR(160) NOT NULL DEFAULT 'Visual analysis of every important factor',
    description    VARCHAR(500),
    media_type     VARCHAR(20)  NOT NULL DEFAULT 'IMAGE',
    media_url      TEXT,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pfp_visual_analysis_floor_plan UNIQUE (floor_plan_id)
);

CREATE INDEX IF NOT EXISTS idx_pfp_visual_analysis_floor_plan
    ON project_floor_plan_visual_analysis (floor_plan_id);

-- ─── project_floor_plan_visual_analysis_tag: colored chip list ──────────────

CREATE TABLE IF NOT EXISTS project_floor_plan_visual_analysis_tag (
    id                  BIGSERIAL    PRIMARY KEY,
    visual_analysis_id  BIGINT       NOT NULL REFERENCES project_floor_plan_visual_analysis (id),
    label               VARCHAR(60)  NOT NULL,
    color               VARCHAR(9)   NOT NULL DEFAULT '#3B7DDD',
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    active              BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_pfp_visual_analysis_tag_parent
    ON project_floor_plan_visual_analysis_tag (visual_analysis_id);
