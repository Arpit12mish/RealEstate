-- Seeds the home_section_config row needed to turn on the TOP_BRANDS section in
-- GET /api/public/home. TopBrandsSectionLoader and HomeSectionType.TOP_BRANDS have existed
-- since Phase 1D, but no config row ever enabled them in any environment, so the section
-- has never actually appeared in a real home feed response.
--
-- Scoped to home_category_id = 0 ("All Home" / global feed), matching the same pattern as
-- V103__add_compare_properties_home_section.sql - this is the category the default
-- GET /api/public/home call resolves to. Not seeded for the other, per-category feed rows
-- (62-74) - those are minimal category-scoped mini-feeds and out of scope here.
--
-- sort_order = 38, i.e. one past the current highest sort_order used by any
-- home_category_id = 0 row (37, GENERIC_CARDS "Top Builders") - appends the section at the
-- end of the existing order instead of renumbering/disturbing anything already configured.
--
-- Idempotent: only inserts if no TOP_BRANDS row already exists for this category (no real
-- unique constraint backs this pair, so this uses the same INSERT ... SELECT ... WHERE NOT
-- EXISTS pattern as V103, not ON CONFLICT). home_section_config has no soft-delete column.

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
  'TOP_BRANDS',
  'Top Brands',
  'Explore trusted brands used across projects',
  TRUE,
  38,
  10,
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'TOP_BRANDS'
);
