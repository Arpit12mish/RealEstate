ALTER TABLE project_connectivity
  ADD COLUMN IF NOT EXISTS summary TEXT,
  ADD COLUMN IF NOT EXISTS default_radius_meters INTEGER,
  ADD COLUMN IF NOT EXISTS search_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE project_connectivity_place
  ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS distance_meters INTEGER,
  ADD COLUMN IF NOT EXISTS duration_seconds INTEGER,
  ADD COLUMN IF NOT EXISTS duration_label VARCHAR(80),
  ADD COLUMN IF NOT EXISTS external_place_id VARCHAR(180),
  ADD COLUMN IF NOT EXISTS provider VARCHAR(40),
  ADD COLUMN IF NOT EXISTS rating NUMERIC(3,2),
  ADD COLUMN IF NOT EXISTS user_rating_count INTEGER,
  ADD COLUMN IF NOT EXISTS address TEXT,
  ADD COLUMN IF NOT EXISTS category VARCHAR(40),
  ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_project_connectivity_place_project_category
  ON project_connectivity_place(project_id, category, active, deleted, sort_order);

CREATE INDEX IF NOT EXISTS idx_project_connectivity_place_project_type
  ON project_connectivity_place(project_id, place_type, active, deleted, sort_order);

CREATE INDEX IF NOT EXISTS idx_project_connectivity_place_external
  ON project_connectivity_place(provider, external_place_id);
