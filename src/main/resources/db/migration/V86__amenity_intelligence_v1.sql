-- Amenity Intelligence v1: adds category, icon, rare/available flags, and visibility controls.
-- All columns default gracefully so existing rows are unaffected.

ALTER TABLE project_amenity_progress
    ADD COLUMN IF NOT EXISTS category               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS category_label         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS icon_key               VARCHAR(80),
    ADD COLUMN IF NOT EXISTS rare                   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS available              BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS public_visible         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS active                 BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS category_display_order INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_project_amenity_project_category
    ON project_amenity_progress (project_id, category, active, public_visible, display_order);

CREATE INDEX IF NOT EXISTS idx_project_amenity_public_lookup
    ON project_amenity_progress (project_id, public_visible, active);
