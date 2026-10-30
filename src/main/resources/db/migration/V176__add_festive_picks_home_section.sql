-- Enables the home "Festive picks" block (FESTIVE_PICKS) on the global home feed
-- (home_category_id = 0). The app renders it directly under the hero, so sort_order is only for
-- dashboard readability; HomeFeedServiceImpl.HOME_SECTION_ORDER keeps it first in the response.
-- Title/subtitle are the editable festive copy: rename them in the dashboard for the next
-- occasion, or set enabled = FALSE to hide the block entirely. Config only; no listing data.
-- Idempotent, same INSERT ... WHERE NOT EXISTS pattern as V175.

INSERT INTO home_section_config (
    home_category_id, section_type, title, subtitle, enabled, sort_order, max_items, created_at, updated_at
)
SELECT
    0,
    'FESTIVE_PICKS',
    'Festive picks on SFS',
    'Verified builders, homes, shops and pros in one place',
    TRUE,
    COALESCE((SELECT MIN(sort_order) - 1 FROM home_section_config WHERE home_category_id = 0), 1),
    8,
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM category WHERE id = 0)
  AND NOT EXISTS (
    SELECT 1 FROM home_section_config
    WHERE home_category_id = 0 AND section_type = 'FESTIVE_PICKS'
);
