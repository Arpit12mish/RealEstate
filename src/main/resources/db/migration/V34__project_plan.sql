CREATE TABLE IF NOT EXISTS project_plan (
  id BIGSERIAL PRIMARY KEY,

  category_id BIGINT NOT NULL REFERENCES category(id),
  builder_id  BIGINT NULL REFERENCES builder(id),

  title VARCHAR(200) NOT NULL,
  description TEXT,
  image_url TEXT NOT NULL,
  company_logo_url TEXT,
  tags TEXT,

  priority INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_project_plan_cat_builder
  ON project_plan(category_id, builder_id, active, deleted, priority);

CREATE INDEX IF NOT EXISTS ix_project_plan_cat_only
  ON project_plan(category_id, active, deleted, priority);
