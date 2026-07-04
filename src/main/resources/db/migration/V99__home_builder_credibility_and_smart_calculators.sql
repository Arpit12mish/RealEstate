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
  'SMART_CALCULATORS',
  'Smart Calculators',
  'Plan costs before you decide',
  TRUE,
  25,
  4,
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'SMART_CALCULATORS'
);

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
  'BUILDER_CREDIBILITY_CARDS',
  'Trusted Builders',
  'Evidence-backed builder reliability',
  TRUE,
  35,
  8,
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'BUILDER_CREDIBILITY_CARDS'
);
