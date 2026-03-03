CREATE TABLE IF NOT EXISTS project_analytics (
  id BIGSERIAL PRIMARY KEY,

  category_id BIGINT NOT NULL REFERENCES category(id),
  builder_id  BIGINT NULL REFERENCES builder(id),

  title VARCHAR(200) NOT NULL,
  image_url TEXT NOT NULL,
  caption VARCHAR(255),

  priority INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_project_analytics_cat_builder
  ON project_analytics(category_id, builder_id, active, deleted, priority);

CREATE INDEX IF NOT EXISTS ix_project_analytics_cat_only
  ON project_analytics(category_id, active, deleted, priority);
