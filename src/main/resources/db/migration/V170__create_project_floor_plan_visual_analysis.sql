-- Visual Floor-Plan Analysis (GAP-027 stabilization): one row per floor plan
-- carrying a title/description/media/tags block, authored by a dashboard
-- operator via ProjectFloorPlanVisualAnalysisServiceImpl. Purely additive -
-- no existing table/column is touched, existing rooms[]/insights[] responses
-- are unaffected.
--
-- Scoped to visual analysis only. Room-comparison ("Space Comparison")
-- columns on project_floor_plan_room_dimension are a separate, still-
-- uncommitted concern (found during this migration's own reconnaissance,
-- out of this phase's explicit scope) and are deliberately NOT included
-- here - see backend-gaps.md for that finding.

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
