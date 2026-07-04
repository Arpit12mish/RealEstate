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
  'NEARBY_LISTINGS',
  'Nearby Listings',
  'Nearby homes and projects',
  TRUE,
  29,
  5,
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'NEARBY_LISTINGS'
);

UPDATE home_section_config
SET sort_order = 29,
    max_items = LEAST(GREATEST(max_items, 1), 10),
    updated_at = NOW()
WHERE home_category_id = 0
  AND section_type = 'NEARBY_LISTINGS';

CREATE INDEX IF NOT EXISTS idx_builder_public
ON builder (published, active, deleted);
