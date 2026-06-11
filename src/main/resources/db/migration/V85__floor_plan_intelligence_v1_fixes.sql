-- Fix: add missing columns to room dimension and insight tables.
-- All new columns are nullable so existing rows are unaffected.

-- Room dimension: soft-delete support + display fields
ALTER TABLE project_floor_plan_room_dimension
    ADD COLUMN IF NOT EXISTS dimension_text VARCHAR(100),
    ADD COLUMN IF NOT EXISTS icon_key       VARCHAR(60),
    ADD COLUMN IF NOT EXISTS active         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deleted        BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pfp_room_dim_floor_plan_active
    ON project_floor_plan_room_dimension (floor_plan_id, active, deleted);

-- Floor plan insight: rich benchmark / chart / visibility fields
ALTER TABLE project_floor_plan_insight
    ADD COLUMN IF NOT EXISTS detailed_text          TEXT,
    ADD COLUMN IF NOT EXISTS unit_value             DECIMAL(12, 4),
    ADD COLUMN IF NOT EXISTS benchmark_value        DECIMAL(12, 4),
    ADD COLUMN IF NOT EXISTS unit_label             VARCHAR(30),
    ADD COLUMN IF NOT EXISTS difference_value       DECIMAL(12, 4),
    ADD COLUMN IF NOT EXISTS difference_percent     DECIMAL(8, 4),
    ADD COLUMN IF NOT EXISTS chart_label_this_unit  VARCHAR(80),
    ADD COLUMN IF NOT EXISTS chart_label_average    VARCHAR(80),
    ADD COLUMN IF NOT EXISTS public_visible         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS verified               BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS active                 BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deleted                BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pfp_insight_floor_plan_public
    ON project_floor_plan_insight (floor_plan_id, public_visible, active, deleted);
