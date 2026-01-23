-- Brand media (banners/gallery/hero)
CREATE TABLE IF NOT EXISTS brand_media (
  id BIGSERIAL PRIMARY KEY,
  brand_id BIGINT NOT NULL,

  media_type VARCHAR(20) NOT NULL,
  placement VARCHAR(20) NOT NULL,

  url TEXT NOT NULL,
  caption VARCHAR(255),

  sort_order INT NOT NULL DEFAULT 0,

  action_type VARCHAR(30),
  action_value TEXT,

  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
);

-- ⚠️ If your Brand table name is "brand", keep this.
-- If your Brand table name is "brands", change it accordingly.
ALTER TABLE brand_media
  ADD CONSTRAINT fk_brand_media_brand
  FOREIGN KEY (brand_id) REFERENCES brand(id);

CREATE INDEX IF NOT EXISTS ix_brand_media_brand_sort
  ON brand_media (brand_id, sort_order);

CREATE INDEX IF NOT EXISTS ix_brand_media_brand_active
  ON brand_media (brand_id, active, deleted);

CREATE INDEX IF NOT EXISTS ix_brand_media_brand_place
  ON brand_media (brand_id, placement);
