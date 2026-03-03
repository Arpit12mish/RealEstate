-- Vxx__home_section_config.sql
CREATE TABLE IF NOT EXISTS home_section_config (
  id BIGSERIAL PRIMARY KEY,

  home_category_id BIGINT NOT NULL REFERENCES category(id),  -- which home category this layout belongs to

  section_type VARCHAR(50) NOT NULL,  -- PROMO_BANNERS, TOP_PROJECTS, TOP_BRANDS, etc.
  title VARCHAR(150),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,

  sort_order INT NOT NULL DEFAULT 0,
  max_items INT NOT NULL DEFAULT 10,

  -- generic params for the section (keep flexible)
  param1 VARCHAR(200),
  param2 VARCHAR(200),
  param3 VARCHAR(200),

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_home_section_config_cat_enabled_sort
  ON home_section_config (home_category_id, enabled, sort_order);
