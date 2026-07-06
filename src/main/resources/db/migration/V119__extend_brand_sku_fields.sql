-- Extends brand_sku (created in V114) with fields required by the Phase 1B
-- admin SKU API: slug (shareable per-brand identity), sku_code (internal
-- reference), a long-form description separate from short_description, and
-- featured/latest flags used to curate "Latest products" on the brand detail
-- page. Purely additive; brand_sku has no rows yet in any environment this
-- has been applied to, so the NOT NULL + unique(brand_id, slug) below carries
-- no backfill risk, but a defensive backfill is included anyway.

ALTER TABLE brand_sku
  ADD COLUMN IF NOT EXISTS slug VARCHAR(180),
  ADD COLUMN IF NOT EXISTS sku_code VARCHAR(80),
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS latest BOOLEAN NOT NULL DEFAULT false;

UPDATE brand_sku SET slug = 'sku-' || id WHERE slug IS NULL;

ALTER TABLE brand_sku
  ALTER COLUMN slug SET NOT NULL;

-- Unique per brand, not globally: two different brands may reasonably want
-- the same slug (e.g. "starter-kit"); SKUs are only ever browsed/managed
-- scoped to their brand.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_brand_sku_brand_slug'
  ) THEN
    ALTER TABLE brand_sku
      ADD CONSTRAINT uk_brand_sku_brand_slug UNIQUE (brand_id, slug);
  END IF;
END $$;
