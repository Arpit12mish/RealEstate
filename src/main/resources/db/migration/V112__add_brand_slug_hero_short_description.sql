-- Adds brand.slug (shareable detail-page URL), hero_image_url (distinct from
-- logo_url), and short_description (card/subtitle copy, separate from the
-- long-form description used for the "Brand overview" section).
--
-- slug is backfilled from name for all existing rows, then locked down to
-- NOT NULL + UNIQUE. Backfill is collision-safe: if two brands would slugify
-- to the same base value, later rows (by id) get a numeric id suffix.

ALTER TABLE brand
  ADD COLUMN IF NOT EXISTS slug VARCHAR(180),
  ADD COLUMN IF NOT EXISTS hero_image_url TEXT,
  ADD COLUMN IF NOT EXISTS short_description VARCHAR(255);

UPDATE brand
SET slug = base.slug
FROM (
  SELECT
    id,
    base_slug || CASE
      WHEN row_number() OVER (PARTITION BY base_slug ORDER BY id) = 1 THEN ''
      ELSE '-' || id
    END AS slug
  FROM (
    SELECT
      id,
      trim(both '-' FROM regexp_replace(lower(trim(name)), '[^a-z0-9]+', '-', 'g')) AS base_slug
    FROM brand
    WHERE slug IS NULL
  ) AS slugified
) AS base
WHERE brand.id = base.id
  AND brand.slug IS NULL;

-- Safety net for the pathological case of an empty/whitespace-only name.
UPDATE brand
SET slug = 'brand-' || id
WHERE slug IS NULL OR slug = '';

ALTER TABLE brand
  ALTER COLUMN slug SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_brand_slug'
  ) THEN
    ALTER TABLE brand
      ADD CONSTRAINT uk_brand_slug UNIQUE (slug);
  END IF;
END $$;
