-- Restore only deterministic reference/configuration rows that a fresh B134
-- database needs for core public flows. Historical demo businesses, brands,
-- companies, banners, pricing matrices, placeholder legal copy, and placeholder
-- contact settings are intentionally not recreated.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM category WHERE id = 0 AND slug <> 'sfs-home-all') THEN
        RAISE EXCEPTION 'V141 cannot reserve category id 0: it belongs to another category';
    END IF;
    IF EXISTS (SELECT 1 FROM category WHERE slug = 'sfs-home-all' AND id <> 0) THEN
        RAISE EXCEPTION 'V141 cannot create category id 0: slug sfs-home-all exists at another id';
    END IF;
END
$$;

INSERT INTO category (id, name, slug, parent_id, priority, active)
VALUES (0, 'All Home', 'sfs-home-all', NULL, 0, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (name, slug, parent_id, priority, active)
SELECT seed.name, seed.slug, NULL, seed.priority, TRUE
FROM (VALUES
    ('Real Estate Services',      'real-estate-services',       1),
    ('Home Décor & Interior',     'home-decor-interior',        2),
    ('Electrical & Automation',   'electrical-automation',      3),
    ('Furniture & Modular',        'furniture-modular',          4),
    ('Sanitary & Bath',            'sanitary-bath',              5),
    ('Flooring & Tiles',           'flooring-tiles',             6),
    ('Hardware & Tools',           'hardware-tools',             7),
    ('Paints & Finishes',          'paints-finishes',            8),
    ('Construction Materials',     'construction-materials',     9),
    ('Home Repair & Maintenance',  'home-repair-maintenance',   10),
    ('Moving & Logistics',         'moving-logistics',          11),
    ('Home Services & Utilities',  'home-services-utilities',   12)
) AS seed(name, slug, priority)
ON CONFLICT (slug) DO NOTHING;

WITH child(parent_slug, name, slug, priority) AS (VALUES
    ('real-estate-services', 'Builders / New Launch / Top Projects / Top Brokers', 'builders-projects-brokers', 1),
    ('real-estate-services', 'Architects', 'architects', 2),
    ('real-estate-services', 'Interior Designers', 'interior-designers', 3),
    ('home-decor-interior', 'Home and Kitchenware', 'home-kitchenware', 1),
    ('home-decor-interior', 'Furnishings', 'furnishings', 2),
    ('home-decor-interior', 'Upholstery', 'upholstery', 3),
    ('electrical-automation', 'Lights', 'lights', 1),
    ('electrical-automation', 'Electrical', 'electrical', 2),
    ('electrical-automation', 'Consumer Durables', 'consumer-durables', 3),
    ('electrical-automation', 'Digital Locks & Safes', 'digital-locks-safes', 4),
    ('electrical-automation', 'Automation', 'automation', 5),
    ('home-repair-maintenance', 'Plumbers', 'plumbers', 1),
    ('home-repair-maintenance', 'Carpenters', 'carpenters', 2),
    ('home-repair-maintenance', 'Electricians', 'electricians', 3),
    ('home-repair-maintenance', 'Masons', 'masons', 4),
    ('home-repair-maintenance', 'Painters', 'painters', 5),
    ('home-repair-maintenance', 'Welders', 'welders', 6),
    ('home-repair-maintenance', 'AC Repair & Services', 'ac-repair-services', 7),
    ('home-repair-maintenance', 'RO Repair Services', 'ro-repair-services', 8),
    ('home-repair-maintenance', 'CCTV Installation', 'cctv-installation', 9),
    ('home-repair-maintenance', 'Appliances Repair', 'appliances-repair', 10),
    ('home-repair-maintenance', 'Home Deep Cleaning', 'home-cleaning', 11),
    ('home-repair-maintenance', 'Pest Control', 'pest-control', 12),
    ('flooring-tiles', 'Tiles & Flooring', 'tiles-flooring', 1),
    ('flooring-tiles', 'Marble & Granite', 'marble-granite', 2),
    ('flooring-tiles', 'Wooden Flooring', 'wooden-flooring', 3),
    ('flooring-tiles', 'Carpet Flooring', 'carpet-flooring', 4),
    ('flooring-tiles', 'Industrial Flooring', 'industrial-flooring', 5),
    ('moving-logistics', 'Movers & Packers', 'movers-packers', 1),
    ('moving-logistics', 'Truck Transport Services', 'truck-transport', 2),
    ('moving-logistics', 'Labor & Loading Services', 'labor-loading', 3),
    ('moving-logistics', 'Courier Services', 'courier-services', 4),
    ('moving-logistics', 'Household Shifting', 'household-shifting', 5),
    ('home-services-utilities', 'Water Tank Cleaning', 'water-tank-cleaning', 1),
    ('home-services-utilities', 'Gas Pipeline Services', 'gas-pipeline-services', 2),
    ('home-services-utilities', 'Solar Panel Installation', 'solar-installation', 3),
    ('home-services-utilities', 'Generator Services', 'generator-services', 4),
    ('home-services-utilities', 'Water Proofing', 'waterproofing', 5),
    ('home-services-utilities', 'Interior Deep Cleaning', 'deep-cleaning', 6),
    ('sanitary-bath', 'Bathroom Fittings', 'bathroom-fittings', 1),
    ('sanitary-bath', 'Water Heaters', 'water-heaters', 2),
    ('sanitary-bath', 'Plumbing Hardware', 'plumbing-hardware', 3),
    ('sanitary-bath', 'Water Tanks', 'water-tanks', 4),
    ('construction-materials', 'Cement', 'cement', 1),
    ('construction-materials', 'Bricks & Blocks', 'bricks-blocks', 2),
    ('construction-materials', 'Sand & Aggregates', 'sand-aggregates', 3),
    ('construction-materials', 'Steel & Rods', 'steel-rods', 4),
    ('construction-materials', 'Ready Mix Concrete', 'ready-mix-concrete', 5)
)
INSERT INTO category (name, slug, parent_id, priority, active)
SELECT child.name, child.slug, parent.id, child.priority, TRUE
FROM child
JOIN category parent ON parent.slug = child.parent_slug
ON CONFLICT (slug) DO NOTHING;

WITH child(name, slug, priority) AS (VALUES
    ('Residential Architects', 'residential-architects', 1),
    ('Commercial Architects', 'commercial-architects', 2),
    ('Interior Architects', 'interior-architects', 3),
    ('Landscape Architects', 'landscape-architects', 4),
    ('3D Elevation Designers', '3d-elevation-designers', 5),
    ('Vastu Architects', 'vastu-architects', 6),
    ('Structural Consultants', 'structural-consultants', 7)
)
INSERT INTO category (name, slug, parent_id, priority, active)
SELECT child.name, child.slug, parent.id, child.priority, TRUE
FROM child
JOIN category parent ON parent.slug = 'architects'
ON CONFLICT (slug) DO NOTHING;

WITH seed(name, slug, state, latitude, longitude) AS (VALUES
    ('New Delhi', 'new-delhi', 'Delhi', 28.6139::double precision, 77.2090::double precision),
    ('Delhi', 'delhi', 'Delhi', 28.7041, 77.1025),
    ('Gurugram', 'gurugram', 'Haryana', 28.4595, 77.0266),
    ('Noida', 'noida', 'Uttar Pradesh', 28.5355, 77.3910),
    ('Greater Noida', 'greater-noida', 'Uttar Pradesh', 28.4744, 77.5040),
    ('Greater Noida West', 'greater-noida-west', 'Uttar Pradesh', 28.6080, 77.4260),
    ('Ghaziabad', 'ghaziabad', 'Uttar Pradesh', 28.6692, 77.4538),
    ('Faridabad', 'faridabad', 'Haryana', 28.4089, 77.3178),
    ('Sonipat', 'sonipat', 'Haryana', 28.9931, 77.0151),
    ('Bahadurgarh', 'bahadurgarh', 'Haryana', 28.6924, 76.9239),
    ('Bhiwadi', 'bhiwadi', 'Rajasthan', 28.2102, 76.8606)
)
INSERT INTO city (name, slug, state, country_code, latitude, longitude, active)
SELECT seed.name, seed.slug, seed.state, 'IN', seed.latitude, seed.longitude, TRUE
FROM seed
WHERE NOT EXISTS (
    SELECT 1 FROM city existing
    WHERE lower(existing.name) = lower(seed.name)
      AND lower(coalesce(existing.state, '')) = lower(seed.state)
)
ON CONFLICT DO NOTHING;

INSERT INTO content_version(key, version) VALUES
    ('HOME', 1),
    ('BRANDS', 1),
    ('BUILDERS', 1),
    ('BUILDER_IMPROVEMENT', 1),
    ('BUILDER_HIGHLIGHT', 1)
ON CONFLICT (key) DO NOTHING;

WITH seed(section_type, title, subtitle, sort_order, max_items, param1) AS (VALUES
    ('SMART_CALCULATORS', 'Smart Calculators', 'Plan costs before you decide', 25, 4, NULL::varchar),
    ('INSTAGRAM_REELS', 'Instagram Reels', 'See our latest reels on instagram', 28, 10, NULL),
    ('NEARBY_LISTINGS', 'Nearby Listings', 'Nearby homes and projects', 29, 5, NULL),
    ('TRENDING_CITIES', 'Trending Cities', 'Hot real estate markets', 30, 10, NULL),
    ('COMPARE_PROPERTIES', 'Compare Properties', 'Compare prices, amenities, approvals and project scores side by side', 32, 1,
     'https://cdn.squarefootstory.com/home/compare-properties/comparision-animation.json'),
    ('BUILDER_CREDIBILITY_CARDS', 'Trusted Builders', 'Evidence-backed builder reliability', 35, 8, NULL),
    ('CONNECTED_BRANDS', 'Connected Brands', 'Explore trusted brands used across projects', 38, 10, NULL)
)
INSERT INTO home_section_config (
    home_category_id, section_type, title, subtitle, enabled,
    sort_order, max_items, param1
)
SELECT 0, seed.section_type, seed.title, seed.subtitle, TRUE,
       seed.sort_order, seed.max_items, seed.param1
FROM seed
WHERE NOT EXISTS (
    SELECT 1 FROM home_section_config existing
    WHERE existing.home_category_id = 0
      AND existing.section_type = seed.section_type
);

INSERT INTO promo_banner_slot_config (
    screen, home_category_id, city_id, slot_key, insert_after_section_type,
    position_index, max_items, priority, is_active
)
SELECT 'HOME', 0, NULL, 'HERO', NULL, 0, 4, 1, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM promo_banner_slot_config
    WHERE screen = 'HOME'
      AND home_category_id = 0
      AND city_id IS NULL
      AND slot_key = 'HERO'
);
