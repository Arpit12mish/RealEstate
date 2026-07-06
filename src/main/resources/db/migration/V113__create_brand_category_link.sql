-- Multi-category tagging for brand filter chips (Paints, Electronics, Furniture, ...).
--
-- Reuses the existing global `category` table rather than introducing a
-- separate brand_category table: `category` already carries a suitable
-- product/material taxonomy (Paint & Finishes, Tiles & Flooring, Building
-- Materials, Electrical & Smart Living, ...). brand.category_id (see V111)
-- remains as the optional single "primary" category for backward
-- compatibility; this table is additive and lets one brand carry several
-- chips at once.

CREATE TABLE IF NOT EXISTS brand_category_link (
  id          BIGSERIAL PRIMARY KEY,
  brand_id    BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  sort_order  INTEGER NOT NULL DEFAULT 0,
  active      BOOLEAN NOT NULL DEFAULT true,
  deleted     BOOLEAN NOT NULL DEFAULT false,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_category_link_brand'
  ) THEN
    ALTER TABLE brand_category_link
      ADD CONSTRAINT fk_brand_category_link_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_category_link_category'
  ) THEN
    ALTER TABLE brand_category_link
      ADD CONSTRAINT fk_brand_category_link_category FOREIGN KEY (category_id) REFERENCES category(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_brand_category_link_brand_category'
  ) THEN
    ALTER TABLE brand_category_link
      ADD CONSTRAINT uk_brand_category_link_brand_category UNIQUE (brand_id, category_id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_category_link_brand_sort ON brand_category_link (brand_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_brand_category_link_category ON brand_category_link (category_id);
