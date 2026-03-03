CREATE TABLE IF NOT EXISTS company_project (
  id BIGSERIAL PRIMARY KEY,

  company_id BIGINT NOT NULL REFERENCES company(id),
  name VARCHAR(180) NOT NULL,
  slug VARCHAR(220) UNIQUE,

  description TEXT,

  city_id BIGINT REFERENCES city(id),
  address_line TEXT,

  cover_media_url TEXT,
  cover_media_type VARCHAR(20),

  active BOOLEAN NOT NULL DEFAULT TRUE,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  priority INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_company_project_company
  ON company_project(company_id);

CREATE INDEX IF NOT EXISTS idx_company_project_pub_active
  ON company_project(published, active, deleted);

CREATE INDEX IF NOT EXISTS idx_company_project_priority
  ON company_project(priority, id);