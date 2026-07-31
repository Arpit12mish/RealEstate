-- Adds builder.slug (canonical public detail-page URL), required to close
-- GAP-002 (BuilderPublicResponse has no slug field at all).
--
-- slug is backfilled from name for all existing rows, then locked down to
-- NOT NULL + UNIQUE. Backfill is collision-safe: if two builders would
-- slugify to the same base value (including case- and punctuation-
-- equivalent names, e.g. "M3M India" / "M3M-India" / "m3m  india" all
-- normalize to the same base_slug), later rows (by id) get a numeric id
-- suffix - identical policy to V112__add_brand_slug_hero_short_description.sql,
-- reused rather than reinvented.
--
-- Deleted/inactive builders are backfilled and constrained identically to
-- active ones - their slugs remain permanently reserved (not reusable by a
-- future builder), matching this codebase's existing brand.slug precedent
-- (the brand unique constraint is not scoped to deleted = false either).

ALTER TABLE builder
  ADD COLUMN IF NOT EXISTS slug VARCHAR(180);

UPDATE builder
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
    FROM builder
    WHERE slug IS NULL
  ) AS slugified
) AS base
WHERE builder.id = base.id
  AND builder.slug IS NULL;

-- Safety net for the pathological case of an empty/whitespace-only name.
UPDATE builder
SET slug = 'builder-' || id
WHERE slug IS NULL OR slug = '';

ALTER TABLE builder
  ALTER COLUMN slug SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_builder_slug'
  ) THEN
    ALTER TABLE builder
      ADD CONSTRAINT uk_builder_slug UNIQUE (slug);
  END IF;
END $$;
