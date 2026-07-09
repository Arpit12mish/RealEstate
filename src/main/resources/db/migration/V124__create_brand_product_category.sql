-- Brand-owned product categories (Phase 2B.3) - e.g. "Lamps", "Mirrors", "Kitchenware" shown
-- on a single brand's detail page, each linking out to that brand's own website. This is a
-- distinct concept from brand_category_link (the global Paints/Electronics/Furniture taxonomy
-- used for Connected Brands chips/filtering) - do not confuse the two.

CREATE TABLE IF NOT EXISTS brand_product_category (
  id              BIGSERIAL PRIMARY KEY,
  brand_id        BIGINT NOT NULL,
  name            VARCHAR(150) NOT NULL,
  slug            VARCHAR(180) NOT NULL,
  description     TEXT,
  image_url       TEXT,
  external_url    TEXT,
  active          BOOLEAN NOT NULL DEFAULT true,
  public_visible  BOOLEAN NOT NULL DEFAULT true,
  sort_order      INTEGER NOT NULL DEFAULT 0,
  deleted         BOOLEAN NOT NULL DEFAULT false,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_product_category_brand'
  ) THEN
    ALTER TABLE brand_product_category
      ADD CONSTRAINT fk_brand_product_category_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;
END $$;

-- Unique per brand among non-deleted rows only - a soft-deleted category's slug must not
-- block re-creating a category with the same slug later (mirrors the brand_collaboration
-- soft-delete-aware unique index approach from V121, not a plain UNIQUE constraint).
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_product_category_brand_slug_active
  ON brand_product_category (brand_id, slug)
  WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_brand_product_category_brand_sort
  ON brand_product_category (brand_id, sort_order, id);
