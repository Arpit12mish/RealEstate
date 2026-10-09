-- Enables the home "Marketplace" carousel (MARKETPLACE_DEALERS) on the global home feed
-- (home_category_id = 0). Its on-screen position - after Smart Calculators, before Instagram
-- Reels - is fixed by HomeFeedServiceImpl.HOME_SECTION_ORDER, so sort_order only needs to be
-- unique-ish; it reuses the Instagram Reels value when present so the dashboard ordering stays
-- readable. Config only: no listing data is inserted. The section is hidden automatically while
-- there are no active dealers. Idempotent, same INSERT ... WHERE NOT EXISTS pattern as V122.

INSERT INTO home_section_config (
    home_category_id, section_type, title, subtitle, enabled, sort_order, max_items, created_at, updated_at
)
SELECT
    0,
    'MARKETPLACE_DEALERS',
    'Marketplace',
    'See nearby marketplace',
    TRUE,
    COALESCE(
        (SELECT MIN(sort_order) FROM home_section_config
         WHERE home_category_id = 0 AND section_type = 'INSTAGRAM_REELS'),
        28),
    10,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM category WHERE id = 0)
  AND NOT EXISTS (
    SELECT 1 FROM home_section_config
    WHERE home_category_id = 0 AND section_type = 'MARKETPLACE_DEALERS'
);
