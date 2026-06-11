-- Floor Plan Intelligence v1: structured fields, room dimensions, and insights.
-- All new columns are nullable (or have defaults) so existing floor plans are unaffected.

-- ─── project_floor_plan: new structured fields ───────────────────────────────

ALTER TABLE project_floor_plan
    ADD COLUMN IF NOT EXISTS unit_configuration_type   VARCHAR(30),
    ADD COLUMN IF NOT EXISTS price                     BIGINT,
    ADD COLUMN IF NOT EXISTS saleable_area_sqft        DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS carpet_area_sqft          DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS built_up_area_sqft        DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS super_area_sqft           DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS floor_height_meters       DECIMAL(5, 2),
    ADD COLUMN IF NOT EXISTS carpet_efficiency_percent DECIMAL(5, 2),
    ADD COLUMN IF NOT EXISTS bedrooms                  INTEGER,
    ADD COLUMN IF NOT EXISTS bathrooms                 INTEGER,
    ADD COLUMN IF NOT EXISTS balconies                 INTEGER,
    ADD COLUMN IF NOT EXISTS facing                    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS direction_summary         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tower_name                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS floor_range               VARCHAR(100),
    ADD COLUMN IF NOT EXISTS key_plan_image_url        TEXT,
    ADD COLUMN IF NOT EXISTS featured                  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS insights_available        BOOLEAN NOT NULL DEFAULT FALSE;

-- ─── project_floor_plan_room_dimension ───────────────────────────────────────

CREATE TABLE IF NOT EXISTS project_floor_plan_room_dimension (
    id             BIGSERIAL    PRIMARY KEY,
    floor_plan_id  BIGINT       NOT NULL REFERENCES project_floor_plan (id),
    room_type      VARCHAR(50)  NOT NULL,
    label          VARCHAR(100),
    length_ft      DECIMAL(8, 2),
    width_ft       DECIMAL(8, 2),
    area_sqft      DECIMAL(10, 2),
    dimension_text VARCHAR(100),
    icon_key       VARCHAR(60),
    notes          VARCHAR(255),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pfp_room_dim_floor_plan_id
    ON project_floor_plan_room_dimension (floor_plan_id);

-- ─── project_floor_plan_insight ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS project_floor_plan_insight (
    id                     BIGSERIAL    PRIMARY KEY,
    floor_plan_id          BIGINT       NOT NULL REFERENCES project_floor_plan (id),
    insight_type           VARCHAR(50)  NOT NULL,
    title                  VARCHAR(160) NOT NULL,
    summary                VARCHAR(500),
    detailed_text          TEXT,
    -- Numeric benchmark comparison
    unit_value             DECIMAL(12, 4),
    benchmark_value        DECIMAL(12, 4),
    unit_label             VARCHAR(30),
    difference_value       DECIMAL(12, 4),
    difference_percent     DECIMAL(8, 4),
    -- Chart display labels
    chart_label_this_unit  VARCHAR(80),
    chart_label_average    VARCHAR(80),
    -- Classification
    comparison_result      VARCHAR(30),
    score                  INTEGER,
    positive               BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Visibility and lifecycle
    public_visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order             INTEGER      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pfp_insight_floor_plan_id
    ON project_floor_plan_insight (floor_plan_id);
