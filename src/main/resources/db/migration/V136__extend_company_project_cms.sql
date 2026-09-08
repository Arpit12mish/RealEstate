ALTER TABLE company_project
  ADD COLUMN IF NOT EXISTS slug VARCHAR(160),
  ADD COLUMN IF NOT EXISTS short_description VARCHAR(300),
  ADD COLUMN IF NOT EXISTS location_label VARCHAR(180),
  ADD COLUMN IF NOT EXISTS stats_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS budget_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN IF NOT EXISTS price_breakdown_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS client_requirements_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS design_materials_json JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE TABLE IF NOT EXISTS company_project_media (
  id BIGSERIAL PRIMARY KEY,
  company_project_id BIGINT NOT NULL REFERENCES company_project(id),
  media_url TEXT NOT NULL,
  media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  category_key VARCHAR(80) NOT NULL,
  category_label VARCHAR(120),
  title VARCHAR(160),
  description TEXT,
  metric_label VARCHAR(80),
  metric_value VARCHAR(120),
  sort_order INT NOT NULL DEFAULT 0,
  public_visible BOOLEAN NOT NULL DEFAULT TRUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_company_project_media_project
  ON company_project_media(company_project_id);

CREATE INDEX IF NOT EXISTS idx_company_project_media_project_category
  ON company_project_media(company_project_id, category_key);

CREATE INDEX IF NOT EXISTS idx_company_project_media_public
  ON company_project_media(company_project_id, active, deleted, public_visible);
