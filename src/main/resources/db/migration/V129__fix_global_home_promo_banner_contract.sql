-- Final contract:
-- Global Home API promo sections are controlled only by promo_banner_slot_config.
-- HERO appears at the top and supports carousel banners.
-- MID remains controlled by slot config.
-- home_section_config must not create global PROMO_BANNERS.

-- Ensure global HOME/HERO slot exists.
INSERT INTO promo_banner_slot_config (
    screen,
    home_category_id,
    city_id,
    slot_key,
    insert_after_section_type,
    position_index,
    max_items,
    priority,
    is_active,
    start_at,
    end_at,
    created_at,
    updated_at
)
SELECT
    'HOME',
    0,
    NULL,
    'HERO',
    NULL,
    0,
    4,
    1,
    TRUE,
    NULL,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM promo_banner_slot_config
    WHERE screen = 'HOME'
      AND home_category_id = 0
      AND slot_key = 'HERO'
);

-- Make existing global HOME/HERO slot produce multiple carousel banners at top.
UPDATE promo_banner_slot_config
SET
    insert_after_section_type = NULL,
    position_index = 0,
    max_items = 4,
    priority = 1,
    is_active = TRUE,
    updated_at = NOW()
WHERE screen = 'HOME'
  AND home_category_id = 0
  AND slot_key = 'HERO';

-- Disable DB-driven global promo section to prevent second HERO section.
UPDATE home_section_config
SET
    enabled = FALSE,
    updated_at = NOW()
WHERE home_category_id = 0
  AND section_type = 'PROMO_BANNERS'
  AND enabled = TRUE;