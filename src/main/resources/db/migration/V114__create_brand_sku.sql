-- Brand SKU / product catalog (e.g. "Berger Silk Glamour", "UltraTech Cement").
-- A dedicated table: brand_media models images and brand_distributor models
-- where-to-buy, neither models product identity.
--
-- category_id points at the same global `category` table used by
-- brand_category_link (see V113) rather than a second taxonomy.

CREATE TABLE IF NOT EXISTS brand_sku (
  id                 BIGSERIAL PRIMARY KEY,
  brand_id           BIGINT NOT NULL,
  category_id        BIGINT,
  name               VARCHAR(150) NOT NULL,
  short_description  VARCHAR(255),
  image_url          TEXT,
  price_label        VARCHAR(60),
  sort_order         INTEGER NOT NULL DEFAULT 0,
  active             BOOLEAN NOT NULL DEFAULT true,
  published          BOOLEAN NOT NULL DEFAULT false,
  priority           INTEGER NOT NULL DEFAULT 0,
  deleted            BOOLEAN NOT NULL DEFAULT false,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_sku_brand'
  ) THEN
    ALTER TABLE brand_sku
      ADD CONSTRAINT fk_brand_sku_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_sku_category'
  ) THEN
    ALTER TABLE brand_sku
      ADD CONSTRAINT fk_brand_sku_category FOREIGN KEY (category_id) REFERENCES category(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_sku_brand_public ON brand_sku (brand_id, published, active, deleted, priority);
CREATE INDEX IF NOT EXISTS idx_brand_sku_category ON brand_sku (category_id);
