-- Adds the brand's own external product-detail-page URL to brand_sku, and an optional link
-- from a SKU to the new brand_product_category (V124) - e.g. a "Table Lamp" SKU can belong
-- to the "Lamps" product category. Both columns are purely additive; brand_sku's existing
-- category_id (global taxonomy) and all other columns are untouched.

ALTER TABLE brand_sku
  ADD COLUMN IF NOT EXISTS external_url TEXT,
  ADD COLUMN IF NOT EXISTS product_category_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_sku_product_category'
  ) THEN
    ALTER TABLE brand_sku
      ADD CONSTRAINT fk_brand_sku_product_category
      FOREIGN KEY (product_category_id) REFERENCES brand_product_category(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_sku_product_category
  ON brand_sku (product_category_id);
