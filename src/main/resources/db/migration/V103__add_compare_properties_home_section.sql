INSERT INTO home_section_config (
  home_category_id,
  section_type,
  title,
  subtitle,
  enabled,
  sort_order,
  max_items,
  param1,
  created_at,
  updated_at
)
SELECT
  0,
  'COMPARE_PROPERTIES',
  'Compare Properties',
  'Compare prices, amenities, approvals and project scores side by side',
  TRUE,
  32,
  1,
  'https://cdn.squarefootstory.com/home/compare-properties/comparision-animation.json',
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'COMPARE_PROPERTIES'
);
