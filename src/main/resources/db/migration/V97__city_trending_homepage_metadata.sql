ALTER TABLE city
  ADD COLUMN IF NOT EXISTS slug VARCHAR(180),
  ADD COLUMN IF NOT EXISTS cover_image_url TEXT,
  ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS homepage_featured BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS growth_percent NUMERIC(6, 2);

WITH normalized AS (
  SELECT
    id,
    NULLIF(
      TRIM(BOTH '-' FROM regexp_replace(lower(trim(name)), '[^a-z0-9]+', '-', 'g')),
      ''
    ) AS base_slug
  FROM city
  WHERE slug IS NULL OR trim(slug) = ''
),
prepared AS (
  SELECT
    id,
    COALESCE(base_slug, 'city-' || id) AS base_slug
  FROM normalized
),
ranked AS (
  SELECT
    id,
    base_slug,
    row_number() OVER (PARTITION BY base_slug ORDER BY id) AS rn
  FROM prepared
)
UPDATE city c
SET slug = CASE
    WHEN r.rn = 1 THEN r.base_slug
    ELSE r.base_slug || '-' || r.rn
  END,
  updated_at = NOW()
FROM ranked r
WHERE c.id = r.id;

ALTER TABLE city
  ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_city_slug
  ON city (lower(slug));

CREATE INDEX IF NOT EXISTS idx_city_homepage_featured
  ON city (active, homepage_featured, display_order, id);

ALTER TABLE home_section_config
  ADD COLUMN IF NOT EXISTS subtitle VARCHAR(255);

INSERT INTO category (
  id,
  name,
  slug,
  parent_id,
  priority,
  active,
  created_at,
  updated_at
)
VALUES (
  0,
  'All Home',
  'sfs-home-all',
  NULL,
  0,
  FALSE,
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    priority = EXCLUDED.priority,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO home_section_config (
  home_category_id,
  section_type,
  title,
  subtitle,
  enabled,
  sort_order,
  max_items,
  created_at,
  updated_at
)
SELECT
  0,
  'TRENDING_CITIES',
  'Trending Cities',
  'Hot real estate markets',
  TRUE,
  30,
  10,
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'TRENDING_CITIES'
);
