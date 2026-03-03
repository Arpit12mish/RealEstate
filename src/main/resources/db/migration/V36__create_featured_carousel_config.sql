CREATE TABLE IF NOT EXISTS featured_carousel_config (
  id              BIGSERIAL PRIMARY KEY,

  city_id         BIGINT NULL,
  category_id     BIGINT NOT NULL,

  -- TALL | SMALL_TOP | SMALL_BOTTOM
  variant         VARCHAR(20) NOT NULL,

  -- order inside section (1..3)
  position        INT NOT NULL,

  title           VARCHAR(180) NOT NULL,
  subtitle        VARCHAR(220) NULL,

  image_url       TEXT NOT NULL,
  logo_url        TEXT NULL,

  -- navigation
  entity_type     VARCHAR(20) NULL,   -- BUILDER | BRAND | DESIGNER | URL
  entity_id       BIGINT NULL,
  target_url      TEXT NULL,

  active          BOOLEAN NOT NULL DEFAULT TRUE,
  priority        INT NOT NULL DEFAULT 0,

  start_at        TIMESTAMPTZ NULL,
  end_at          TIMESTAMPTZ NULL,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- avoid duplicates per category & variant
CREATE UNIQUE INDEX IF NOT EXISTS ux_featured_carousel_cat_variant
ON featured_carousel_config (city_id, category_id, variant)
WHERE active = true;

-- fast lookup
CREATE INDEX IF NOT EXISTS ix_featured_carousel_lookup
ON featured_carousel_config (category_id, city_id, active, priority, position);

-- optional: ensure valid variant values (Postgres CHECK constraint)
ALTER TABLE featured_carousel_config
  ADD CONSTRAINT chk_featured_carousel_variant
  CHECK (variant IN ('TALL','SMALL_TOP','SMALL_BOTTOM'));

ALTER TABLE featured_carousel_config
  ADD CONSTRAINT chk_featured_carousel_position
  CHECK (position BETWEEN 1 AND 3);
