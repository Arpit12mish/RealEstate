-- Lets a brand_media row optionally belong to a specific SKU's gallery
-- instead of the brand as a whole (e.g. individual product photos).
--
-- No change is needed to allow a PRODUCT (or LOGO) placement value: the
-- existing `placement` column is a plain VARCHAR(20) with no CHECK
-- constraint (validity is enforced only by the Java Placement enum), so new
-- enum values fit without a DDL change here.

ALTER TABLE brand_media
  ADD COLUMN IF NOT EXISTS brand_sku_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_media_sku'
  ) THEN
    ALTER TABLE brand_media
      ADD CONSTRAINT fk_brand_media_sku FOREIGN KEY (brand_sku_id) REFERENCES brand_sku(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_media_sku ON brand_media (brand_sku_id);
