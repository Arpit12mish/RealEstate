INSERT INTO featured_carousel_config
(city_id, category_id, enabled, position, variant, title, subtitle, image_url, logo_url, entity_type, entity_id, target_url, sort_order)
VALUES
(1, 62, true, 1, 'TALL', 'Our Expertise Our Joy!', null,
 'https://cdn.sfs.app/featured/m3m-main.png',
 'https://cdn.sfs.app/logos/m3m.png',
 'BUILDER', 2, null, 0),

(1, 62, true, 2, 'SMALL_TOP', 'The better way Home!', null,
 'https://cdn.sfs.app/featured/ats.png',
 'https://cdn.sfs.app/logos/ats.png',
 'BUILDER', 1, null, 0),

(1, 62, true, 3, 'SMALL_BOTTOM', 'Add Prestige to your Life!', null,
 'https://cdn.sfs.app/featured/prestige.png',
 'https://cdn.sfs.app/logos/prestige.png',
 'BUILDER', 4, null, 0)
ON CONFLICT DO NOTHING;
