-- Marketplace demo content (New Delhi), loaded once by MarketplaceDemoSeedRunner when
-- sfs.marketplace.demo-seed.enabled=true (production default). Same stores, photos, products,
-- services, hours, workers and connections as infra/local-staging/fixtures/marketplace-demo-delhi.sql,
-- except:
--   * every demo store/worker uses the single configured contact number (demo_cfg.contact_phone,
--     from SFS_DEMO_CONTACT_PHONE); NULL hides the Call/WhatsApp buttons
--   * no reviews are seeded - ratings come from real users
--   * each demo worker needs its own login account; its number comes from demo_worker_phone
--     (SFS_DEMO_WORKER_LOGIN_PHONES, in MarketplaceDemoSeedRunner.WORKERS order) and must be a
--     mobile your company controls - whoever owns it can sign in as that worker. A worker without
--     a number (or whose number already has an account) is skipped.
--
-- Stores are created once: nothing about them is re-created or overwritten when demo rows
-- (business.website tag) already exist, so restarts never duplicate or reset them and dashboard
-- edits are kept. The five older V6 sample stores are matched by name in New Delhi and only
-- decorated while they have no photos, hours or offerings.
-- Later starts only fill in what is still missing: the contact number on demo stores/workers that
-- have none, and demo workers (with their charges, areas and connections) not created yet.
-- The runner executes this file in ONE transaction, after creating the demo_cfg and
-- demo_worker_phone temp tables.
--
-- Store photos are bundled with the backend (src/main/resources/demo-images/marketplace) and served
-- at <image base>/api/public/demo-images/marketplace/<key>.jpg, because the website only shows images
-- from SFS's own hosts. figma-*.jpg are the Figma "Spotlight" photos (infra/demo-images); the rest
-- are openly licensed copies (Wikimedia Commons / Flickr via Openverse, resized to 1024px), which
-- were hot-linked before. Without an image base URL the hot-links are still used. Sources:
--   paint_colour_wall  CC BY-SA 4.0 | Husskeyy | https://commons.wikimedia.org/wiki/File:Paint_Section_in_a_Builders_Warehouse_Store,_in_Kirstenhof,_Cape_Town.jpg
--   paint_store        CC0 | Daderot | https://commons.wikimedia.org/wiki/File:Paint_store_-_Arlington,_MA.jpg
--   paint_shelves      CC BY-SA 2.0 | ShangChieh | https://commons.wikimedia.org/wiki/File:%E9%8A%80%E5%BA%A7_%E9%80%8F%E6%98%8E%E6%B0%B4%E5%BD%A9%E7%B5%B5%E5%85%B7_2016_(29153802122).jpg
--   spray_shelves      by-sa | davidmulder61 | https://www.flickr.com/photos/113026679@N03/38448295731
--   brush_valspar      by-sa | pmarkham | https://www.flickr.com/photos/9197427@N06/5418858998
--   paint_tins         by | Ms..G | https://www.flickr.com/photos/49793563@N00/483718392
--   brush_on_tin       by | Alan Cleaver | https://www.flickr.com/photos/11121568@N06/4121413003
--   roller_tray        by-sa | Photocapy | https://www.flickr.com/photos/81635051@N00/146214216
--   rollers            by | Kristoffer M.C. | https://www.flickr.com/photos/35381896@N05/4336441079
--   brush_tray         by | Emily Rachel Poisel | https://www.flickr.com/photos/59773112@N07/5991233014
--   screws_bins        CC BY 3.0 | Marek Ślusarczyk (Tupungato) <a rel="no | https://commons.wikimedia.org/wiki/File:011_Screws_and_bolts_in_OBI_hardware_store_home_improvement_retail_store_in_Poland.jpg
--   hardware_counter   CC BY-SA 4.0 | Fredericknoronha | https://commons.wikimedia.org/wiki/File:At_the_hardware_store,_Panjim,_Goa,_circa_2018_08.jpg
--   hardware_shelves   CC BY-SA 4.0 | Fredericknoronha | https://commons.wikimedia.org/wiki/File:At_the_hardware_store,_Panjim,_Goa,_circa_2018_13.jpg
--   tile_showroom      CC0 | Stilfehler | https://commons.wikimedia.org/wiki/File:Tile_Store_1.jpg
--   tile_showroom2     CC0 | Stilfehler | https://commons.wikimedia.org/wiki/File:Tile_Store_2.jpg
--   pipe_fittings      Public domain | Myke2020 | https://commons.wikimedia.org/wiki/File:Copper_pipe_8072.JPG
--   pvc_pipes          CC0 | <a href="//commons.wikimedia.org/w/index | https://commons.wikimedia.org/wiki/File:Circular_pipes.jpg
--   copper_valve       CC BY 2.0 | Tony Webster | https://commons.wikimedia.org/wiki/File:Copper_Water_Pipes_and_Water_Line_Shutoff.jpg
--   copper_fittings    CC BY-SA 3.0 | <a href="//commons.wikimedia.org/w/index | https://commons.wikimedia.org/wiki/File:First_attempt_for_copper_stand_offs.JPG
--   led_bulbs          CC BY 3.0 | Geoffr | https://commons.wikimedia.org/wiki/File:LED_bulbs.jpg
--   switch_board       CC BY-SA 3.0 | Dmitry G | https://commons.wikimedia.org/wiki/File:Light_switches_in_billiard_hall.JPG
--   switch_modern      CC BY-SA 4.0 | Santeri Viinamäki | https://commons.wikimedia.org/wiki/File:White_light_switch.jpg
--   cement_shop        CC BY-SA 4.0 | <a href="//commons.wikimedia.org/w/index | https://commons.wikimedia.org/wiki/File:Vente_de_ciment.jpg

SELECT pg_advisory_xact_lock(74176001);

CREATE TEMP TABLE seed_go ON COMMIT DROP AS
SELECT 1 AS go
WHERE EXISTS (SELECT 1 FROM city WHERE slug = 'new-delhi')
  AND NOT EXISTS (SELECT 1 FROM business WHERE website = 'https://fixtures.sfs.local/marketplace');

CREATE TEMP TABLE seed_ref ON COMMIT DROP AS
SELECT (SELECT id FROM city WHERE slug = 'new-delhi') AS city_id,
       COALESCE((SELECT id FROM category WHERE slug = 'paint-finishes' AND active),
                (SELECT id FROM category WHERE slug = 'paints-finishes')) AS paint_cat,
       (SELECT min(id) FROM dashboard_users) AS reviewer_id,
       (SELECT contact_phone FROM demo_cfg) AS phone;

CREATE TEMP TABLE img (key text PRIMARY KEY, url text NOT NULL) ON COMMIT DROP;
INSERT INTO img (key, url) VALUES
    ('paint_colour_wall', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6f/Paint_Section_in_a_Builders_Warehouse_Store%2C_in_Kirstenhof%2C_Cape_Town.jpg/1280px-Paint_Section_in_a_Builders_Warehouse_Store%2C_in_Kirstenhof%2C_Cape_Town.jpg'),
    ('paint_store', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Paint_store_-_Arlington%2C_MA.jpg/1280px-Paint_store_-_Arlington%2C_MA.jpg'),
    ('paint_shelves', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0f/%E9%8A%80%E5%BA%A7_%E9%80%8F%E6%98%8E%E6%B0%B4%E5%BD%A9%E7%B5%B5%E5%85%B7_2016_%2829153802122%29.jpg/1280px-%E9%8A%80%E5%BA%A7_%E9%80%8F%E6%98%8E%E6%B0%B4%E5%BD%A9%E7%B5%B5%E5%85%B7_2016_%2829153802122%29.jpg'),
    ('spray_shelves', 'https://live.staticflickr.com/4517/38448295731_c7d214d704_b.jpg'),
    ('brush_valspar', 'https://live.staticflickr.com/5218/5418858998_ef574b7a05_b.jpg'),
    ('paint_tins', 'https://live.staticflickr.com/221/483718392_0583320834_b.jpg'),
    ('brush_on_tin', 'https://live.staticflickr.com/2487/4121413003_6912624249_b.jpg'),
    ('roller_tray', 'https://live.staticflickr.com/56/146214216_36231976e7_b.jpg'),
    ('rollers', 'https://live.staticflickr.com/2728/4336441079_7b0f04083b_b.jpg'),
    ('brush_tray', 'https://live.staticflickr.com/6149/5991233014_0efec32224_b.jpg'),
    ('screws_bins', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d9/011_Screws_and_bolts_in_OBI_hardware_store_home_improvement_retail_store_in_Poland.jpg/1280px-011_Screws_and_bolts_in_OBI_hardware_store_home_improvement_retail_store_in_Poland.jpg'),
    ('hardware_counter', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9b/At_the_hardware_store%2C_Panjim%2C_Goa%2C_circa_2018_08.jpg/1280px-At_the_hardware_store%2C_Panjim%2C_Goa%2C_circa_2018_08.jpg'),
    ('hardware_shelves', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4d/At_the_hardware_store%2C_Panjim%2C_Goa%2C_circa_2018_13.jpg/1280px-At_the_hardware_store%2C_Panjim%2C_Goa%2C_circa_2018_13.jpg'),
    ('tile_showroom', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1b/Tile_Store_1.jpg/1280px-Tile_Store_1.jpg'),
    ('tile_showroom2', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bf/Tile_Store_2.jpg/1280px-Tile_Store_2.jpg'),
    ('pipe_fittings', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Copper_pipe_8072.JPG/1280px-Copper_pipe_8072.JPG'),
    ('pvc_pipes', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/dc/Circular_pipes.jpg/1280px-Circular_pipes.jpg'),
    ('copper_valve', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ce/Copper_Water_Pipes_and_Water_Line_Shutoff.jpg/1280px-Copper_Water_Pipes_and_Water_Line_Shutoff.jpg'),
    ('copper_fittings', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fe/First_attempt_for_copper_stand_offs.JPG/1280px-First_attempt_for_copper_stand_offs.JPG'),
    ('led_bulbs', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/76/LED_bulbs.jpg/1280px-LED_bulbs.jpg'),
    ('switch_board', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/Light_switches_in_billiard_hall.JPG/1280px-Light_switches_in_billiard_hall.JPG'),
    ('switch_modern', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/White_light_switch.jpg/1280px-White_light_switch.jpg'),
    ('cement_shop', 'https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e8/Vente_de_ciment.jpg/1280px-Vente_de_ciment.jpg');

-- Bundled copies replace the hot-links; img_move also lets the top-up below repoint photos stored
-- with a hot-link on an earlier start.
CREATE TEMP TABLE img_move ON COMMIT DROP AS
SELECT key, url AS hotlink,
       (SELECT image_base FROM demo_cfg) || '/api/public/demo-images/marketplace/' || replace(key, '_', '-') || '.jpg' AS local_url
FROM img
WHERE (SELECT image_base FROM demo_cfg) IS NOT NULL;
UPDATE img SET url = m.local_url FROM img_move m WHERE m.key = img.key;

-- Figma "Spotlight" photos, bundled only (no hot-link), so only available with an image base URL.
INSERT INTO img (key, url)
SELECT f.key, (SELECT image_base FROM demo_cfg) || '/api/public/demo-images/' || f.path
FROM (VALUES
    ('fig_paint_aisle', 'store-1.jpg'),
    ('fig_bath_showroom', 'store-2.jpg'),
    ('fig_electrical_counter', 'store-3.jpg'),
    ('fig_hardware_wall', 'store-4.jpg'),
    ('fig_living_room', 'marketplace/figma-interior-designers-1.jpg'),
    ('fig_carved_lounge', 'marketplace/figma-interior-designers-2.jpg'),
    ('fig_bedroom', 'marketplace/figma-interior-designers-3.jpg'),
    ('fig_paint_samples', 'marketplace/figma-brands-1.jpg'),
    ('fig_marble_tiles', 'marketplace/figma-brands-2.jpg'),
    ('fig_pendant_lights', 'marketplace/figma-brands-3.jpg'),
    ('fig_modular_kitchen', 'marketplace/figma-brands-4.jpg')
) AS f(key, path)
WHERE (SELECT image_base FROM demo_cfg) IS NOT NULL;

CREATE TEMP TABLE fx (key text PRIMARY KEY, id bigint NOT NULL) ON COMMIT DROP;

-- ---------- older V6 sample stores to decorate (untouched ones only) ----------
INSERT INTO fx (key, id)
SELECT e.key, min(b.id)
FROM (VALUES
    ('d_brightshine', 'Bright Shine Plumbing Services'),
    ('d_superfix', 'SuperFix Plumbing'),
    ('d_master_elec', 'Delhi Master Electricians'),
    ('d_tilezone', 'TileZone Studio'),
    ('d_trimurti', 'Trimurti Wall Care Products Pvt Ltd')
) AS e(key, name)
JOIN business b ON b.name = e.name AND b.city_id = (SELECT city_id FROM seed_ref)
WHERE EXISTS (SELECT 1 FROM seed_go)
  AND NOT EXISTS (SELECT 1 FROM business_media m WHERE m.business_id = b.id)
  AND NOT EXISTS (SELECT 1 FROM business_opening_hours h WHERE h.business_id = b.id)
  AND NOT EXISTS (SELECT 1 FROM business_offering_group g WHERE g.business_id = b.id)
GROUP BY e.key;

-- ---------- new demo dealers ----------
WITH rows(key, name, cat, locality, landmark, est, prio, descr) AS (VALUES
    ('d_main', 'Sharma Paints & Hardware', 'paint', 'Lajpat Nagar', 'Near Central Market', 2008, 99,
     'A local paint and hardware supplier serving homeowners, contractors, and interior professionals with paints, waterproofing solutions, hardware, and related building products.'),
    ('d_elite', 'Elite Home Solutions', 'paint', 'Rajouri Garden', 'Opp. Metro Pillar 412', 2012, 95,
     'Decorative paints, sprayers and waterproof coatings for homes and offices, with on-site colour consultation.'),
    ('d_propaint', 'ProPaint Colour Studio', 'paint', 'Karol Bagh', 'Ajmal Khan Road', 2015, 93,
     'Colour studio with computerised tinting, rollers, brushes and wood finishes for painters and DIY customers.'),
    ('d_hardware', 'Delhi Tools & Hardware Mart', 'hardware-tools', 'Chandni Chowk', 'Bhagirath Palace', 1996, 91,
     'One of Old Delhi''s busiest hardware counters: power tools, hand tools, fasteners, locks and door fittings in retail and bulk.')
), ins AS (
    INSERT INTO business (name, city_id, category_id, locality, landmark, primary_phone, whatsapp_phone, established_year,
                          is_active, description, website, timezone, avg_rating, total_ratings,
                          sponsored, sponsored_priority, sponsored_start, sponsored_end, created_at, updated_at)
    SELECT r.name, s.city_id,
           CASE WHEN r.cat = 'paint' THEN s.paint_cat ELSE (SELECT id FROM category WHERE slug = r.cat) END,
           r.locality, r.landmark, s.phone, s.phone, r.est, TRUE, r.descr,
           'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0,
           TRUE, r.prio, now() - interval '1 day', now() + interval '1 year', now(), now()
    FROM rows r CROSS JOIN seed_ref s
    WHERE EXISTS (SELECT 1 FROM seed_go)
      AND CASE WHEN r.cat = 'paint' THEN s.paint_cat ELSE (SELECT id FROM category WHERE slug = r.cat) END IS NOT NULL
    RETURNING id, name
)
INSERT INTO fx (key, id) SELECT r.key, ins.id FROM rows r JOIN ins ON ins.name = r.name;

-- ---------- demo dealers added in a later release ----------
-- Created on whichever start first sees them missing (by name), so production gets them on the
-- next deploy without touching the stores loaded earlier.
WITH rows(key, name, cat, cat_fallback, locality, landmark, est, prio, descr) AS (VALUES
    ('d_urbannest', 'Urban Nest Furniture Studio', 'luxury-furniture', 'furniture-modular', 'Kirti Nagar', 'Furniture Market, Block C', 2010, 92,
     'Sofas, beds, dining sets and made-to-measure modular furniture, with a design desk that plans full rooms and delivers across Delhi NCR.'),
    ('d_lumina', 'Lumina Electricals & Lighting', 'electrical-smart-living', 'electrical', 'Lajpat Rai Market', 'Near Red Fort Metro Gate 3', 2005, 90,
     'Wires, switches, MCBs, decorative lighting and smart-home devices for electricians, builders and homeowners, in retail and bulk.'),
    ('d_stonecraft', 'StoneCraft Tiles & Bath Gallery', 'tiles-flooring', NULL, 'Mehrauli-Gurgaon Road', 'Opp. Sultanpur Metro', 2013, 88,
     'Large-format tiles, Italian marble, wooden flooring and designer bath fittings in a walk-through showroom.'),
    ('d_aqualine', 'AquaLine Sanitary Hub', 'sanitary-bathroom', 'plumbing-hardware', 'Paharganj', 'DB Gupta Road', 2009, 86,
     'Sanitaryware, CPVC and PVC pipes, taps, showers and water heaters, with plumbers on call for installation.')
), resolved AS (
    SELECT r.*, COALESCE((SELECT id FROM category WHERE slug = r.cat AND active), (SELECT id FROM category WHERE slug = r.cat_fallback)) AS cat_id
    FROM rows r
), ins AS (
    INSERT INTO business (name, city_id, category_id, locality, landmark, primary_phone, whatsapp_phone, established_year,
                          is_active, description, website, timezone, avg_rating, total_ratings,
                          sponsored, sponsored_priority, sponsored_start, sponsored_end, created_at, updated_at)
    SELECT r.name, s.city_id, r.cat_id, r.locality, r.landmark, s.phone, s.phone, r.est, TRUE, r.descr,
           'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0,
           TRUE, r.prio, now() - interval '1 day', now() + interval '1 year', now(), now()
    FROM resolved r CROSS JOIN seed_ref s
    WHERE s.city_id IS NOT NULL AND r.cat_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM business b WHERE b.name = r.name AND b.city_id = s.city_id)
    RETURNING id, name
)
INSERT INTO fx (key, id) SELECT r.key, ins.id FROM rows r JOIN ins ON ins.name = r.name;

-- ---------- decorate the older sample stores (contact numbers are left as they are) ----------
UPDATE business b SET
    locality = v.locality,
    description = v.descr,
    established_year = COALESCE(b.established_year, v.est),
    timezone = 'Asia/Kolkata',
    sponsored = TRUE, sponsored_priority = v.prio,
    sponsored_start = now() - interval '1 day', sponsored_end = now() + interval '1 year',
    updated_at = now()
FROM (VALUES
    ('d_brightshine', 'Laxmi Nagar', 2017, 89, 'Licensed plumbing contractors for leak repairs, bathroom fittings and complete CPVC re-piping, with emergency visits across East Delhi.'),
    ('d_superfix', 'Mayur Vihar', 2020, 83, 'Taps, mixers, pipes and valves with quick pipe replacement and drain cleaning services.'),
    ('d_master_elec', 'Patel Nagar', 2016, 87, 'Electrical supplies and certified electricians for house wiring, lighting, MCB panels and inverter installation.'),
    ('d_tilezone', 'Kirti Nagar', 2011, 85, 'Tile and flooring showroom with vitrified, ceramic, wooden and marble options, plus free layout design.'),
    ('d_trimurti', 'Okhla Phase II', 2017, 81, 'Wall putty, cement and tile adhesives supplied to contractors and retail customers across South Delhi.')
) AS v(key, locality, est, prio, descr)
JOIN fx ON fx.key = v.key
WHERE b.id = fx.id;

-- ---------- opening hours (day 1 = Monday) ----------
INSERT INTO business_opening_hours (business_id, day_of_week, opens_at, closes_at)
SELECT fx.id, d, h.opens::time, h.closes::time
FROM (VALUES
    ('d_main', 1, 6, '09:00', '22:00'), ('d_main', 7, 7, '10:00', '14:00'),
    ('d_elite', 1, 7, '10:00', '19:45'),
    ('d_propaint', 1, 6, '09:30', '20:30'),
    ('d_hardware', 1, 6, '10:00', '19:00'),
    ('d_brightshine', 1, 6, '08:00', '20:00'),
    ('d_superfix', 1, 7, '08:00', '21:00'),
    ('d_master_elec', 1, 6, '09:00', '21:00'),
    ('d_tilezone', 2, 7, '10:30', '20:00'),
    ('d_trimurti', 1, 6, '09:00', '18:30'),
    ('d_urbannest', 1, 7, '10:30', '20:30'),
    ('d_lumina', 1, 6, '10:00', '19:30'),
    ('d_stonecraft', 2, 7, '10:00', '20:00'),
    ('d_aqualine', 1, 6, '09:30', '20:00')
) AS h(key, from_day, to_day, opens, closes)
JOIN fx ON fx.key = h.key
CROSS JOIN LATERAL generate_series(h.from_day, h.to_day) AS d;

-- ---------- products and services ----------
CREATE TEMP TABLE off_src (key text, kind text, title text, gord int, items text[]) ON COMMIT DROP;
INSERT INTO off_src VALUES
    ('d_main', 'PRODUCT', 'Paints', 0, ARRAY['Interior Emulsion', 'Exterior Emulsion', 'Enamel Paint', 'Wood Polish', 'Wall Putty', 'Primers']),
    ('d_main', 'PRODUCT', 'Hardware', 1, ARRAY['Door Hardware', 'Locks', 'Fasteners', 'Hand Tools']),
    ('d_main', 'PRODUCT', 'Waterproofing', 2, ARRAY['Roof Coatings', 'Damp Proofing', 'Sealants']),
    ('d_main', 'SERVICE', 'Services', 0, ARRAY['Color Consultation', 'Paint Matching', 'Home Delivery', 'Contractor Support', 'Bulk Orders', 'Installation Support']),
    ('d_elite', 'PRODUCT', 'Paints', 0, ARRAY['Sprayers', 'Waterproof Coatings', 'Primers', 'Texture Paints']),
    ('d_elite', 'SERVICE', 'Services', 0, ARRAY['Colour Consultation', 'Site Visits', 'Home Delivery']),
    ('d_propaint', 'PRODUCT', 'Painting Supplies', 0, ARRAY['Rollers', 'Brushes', 'Enamels', 'Wood Finishes', 'Masking Tape']),
    ('d_propaint', 'SERVICE', 'Services', 0, ARRAY['Computerised Tinting', 'Shade Matching']),
    ('d_hardware', 'PRODUCT', 'Tools & Hardware', 0, ARRAY['Power Tools', 'Hand Tools', 'Fasteners', 'Locks', 'Door Fittings']),
    ('d_hardware', 'SERVICE', 'Services', 0, ARRAY['Key Cutting', 'Bulk Orders', 'Home Delivery']),
    ('d_brightshine', 'PRODUCT', 'Pipes & Fittings', 0, ARRAY['CPVC Pipes', 'PVC Pipes', 'Copper Fittings', 'Ball Valves']),
    ('d_brightshine', 'SERVICE', 'Services', 0, ARRAY['Leak Repair', 'Bathroom Fitting', 'Water Tank Cleaning', 'Emergency Visits']),
    ('d_superfix', 'PRODUCT', 'Bath & Plumbing', 0, ARRAY['Taps', 'Mixers', 'Pipes', 'Valves']),
    ('d_superfix', 'SERVICE', 'Services', 0, ARRAY['Pipe Replacement', 'Drain Cleaning']),
    ('d_master_elec', 'PRODUCT', 'Electricals', 0, ARRAY['LED Bulbs', 'Switches', 'Wires & Cables', 'MCBs']),
    ('d_master_elec', 'SERVICE', 'Services', 0, ARRAY['House Wiring', 'Fan & Light Fitting', 'Inverter Setup']),
    ('d_tilezone', 'PRODUCT', 'Tiles & Flooring', 0, ARRAY['Vitrified Tiles', 'Ceramic Tiles', 'Wooden Flooring', 'Marble']),
    ('d_tilezone', 'SERVICE', 'Services', 0, ARRAY['Layout Design', 'Home Delivery']),
    ('d_trimurti', 'PRODUCT', 'Building Materials', 0, ARRAY['Wall Putty', 'Cement', 'White Cement', 'Tile Adhesive']),
    ('d_urbannest', 'PRODUCT', 'Furniture', 0, ARRAY['Sofas', 'Beds', 'Dining Sets', 'Wardrobes', 'TV Units']),
    ('d_urbannest', 'PRODUCT', 'Modular', 1, ARRAY['Modular Kitchens', 'Study Units', 'Shoe Racks']),
    ('d_urbannest', 'SERVICE', 'Services', 0, ARRAY['Room Planning', 'Custom Sizes', 'Home Delivery', 'Installation']),
    ('d_lumina', 'PRODUCT', 'Electricals', 0, ARRAY['Wires & Cables', 'Modular Switches', 'MCBs & DBs', 'Fans']),
    ('d_lumina', 'PRODUCT', 'Lighting', 1, ARRAY['LED Panels', 'Pendant Lights', 'Smart Bulbs', 'Outdoor Lights']),
    ('d_lumina', 'SERVICE', 'Services', 0, ARRAY['Lighting Design', 'Bulk Orders', 'Electrician on Call']),
    ('d_stonecraft', 'PRODUCT', 'Tiles & Stone', 0, ARRAY['Large-format Tiles', 'Italian Marble', 'Wooden Flooring', 'Mosaic']),
    ('d_stonecraft', 'PRODUCT', 'Bath', 1, ARRAY['Wall-hung WCs', 'Basins', 'Rain Showers']),
    ('d_stonecraft', 'SERVICE', 'Services', 0, ARRAY['3D Layout', 'Site Measurement', 'Home Delivery']),
    ('d_aqualine', 'PRODUCT', 'Sanitary & Plumbing', 0, ARRAY['Sanitaryware', 'CPVC Pipes', 'Taps & Mixers', 'Water Heaters']),
    ('d_aqualine', 'SERVICE', 'Services', 0, ARRAY['Installation', 'Plumber on Call', 'Home Delivery']);

WITH g AS (
    INSERT INTO business_offering_group (business_id, offering_type, title, sort_order)
    SELECT fx.id, s.kind, s.title, s.gord FROM off_src s JOIN fx ON fx.key = s.key
    RETURNING id, business_id, offering_type, title
)
INSERT INTO business_offering_item (group_id, name, sort_order)
SELECT g.id, item.name, item.ord - 1
FROM g
JOIN fx ON fx.id = g.business_id
JOIN off_src s ON s.key = fx.key AND s.kind = g.offering_type AND s.title = g.title
CROSS JOIN LATERAL unnest(s.items) WITH ORDINALITY AS item(name, ord);

-- ---------- photos (HERO = top carousel / card cover, GALLERY = "Store Photos") ----------
INSERT INTO business_media (business_id, usage_type, media_url, alt_text, sort_order)
SELECT fx.id, m.usage, img.url, m.alt, m.ord
FROM (VALUES
    ('d_main', 'HERO', 'paint_colour_wall', 'Colour shade wall', 0),
    ('d_main', 'HERO', 'paint_store', 'Inside the store', 1),
    ('d_main', 'HERO', 'spray_shelves', 'Spray paint shelves', 2),
    ('d_main', 'HERO', 'paint_shelves', 'Paint shelves', 3),
    ('d_main', 'GALLERY', 'paint_tins', 'Paint tins', 0),
    ('d_main', 'GALLERY', 'brush_on_tin', 'Brush on a paint tin', 1),
    ('d_main', 'GALLERY', 'roller_tray', 'Roller and tray', 2),
    ('d_main', 'GALLERY', 'rollers', 'Paint rollers', 3),
    ('d_main', 'GALLERY', 'brush_valspar', 'Paint sample and brushes', 4),
    ('d_main', 'GALLERY', 'brush_tray', 'Brush and tray', 5),
    ('d_main', 'GALLERY', 'screws_bins', 'Fasteners section', 6),
    ('d_main', 'GALLERY', 'paint_store', 'Store aisle', 7),
    ('d_main', 'GALLERY', 'spray_shelves', 'Spray paints', 8),
    ('d_main', 'GALLERY', 'paint_colour_wall', 'Shade cards', 9),
    ('d_elite', 'HERO', 'spray_shelves', 'Spray paint range', 0),
    ('d_elite', 'HERO', 'paint_colour_wall', 'Shade wall', 1),
    ('d_elite', 'GALLERY', 'roller_tray', 'Roller and tray', 0),
    ('d_elite', 'GALLERY', 'paint_tins', 'Paint tins', 1),
    ('d_propaint', 'HERO', 'paint_tins', 'Tinted paints', 0),
    ('d_propaint', 'HERO', 'rollers', 'Rollers', 1),
    ('d_propaint', 'GALLERY', 'brush_on_tin', 'Brushes', 0),
    ('d_propaint', 'GALLERY', 'brush_valspar', 'Sample pots', 1),
    ('d_hardware', 'HERO', 'screws_bins', 'Fastener bins', 0),
    ('d_hardware', 'HERO', 'hardware_shelves', 'Hardware shelves', 1),
    ('d_hardware', 'GALLERY', 'hardware_counter', 'Sales counter', 0),
    ('d_hardware', 'GALLERY', 'hardware_shelves', 'Stock room', 1),
    ('d_brightshine', 'HERO', 'pipe_fittings', 'Pipe fittings display', 0),
    ('d_brightshine', 'HERO', 'copper_valve', 'Copper pipe and valve', 1),
    ('d_brightshine', 'GALLERY', 'pvc_pipes', 'PVC pipes', 0),
    ('d_brightshine', 'GALLERY', 'copper_fittings', 'Copper fittings', 1),
    ('d_superfix', 'HERO', 'pvc_pipes', 'PVC pipes in stock', 0),
    ('d_superfix', 'GALLERY', 'copper_fittings', 'Copper fittings', 0),
    ('d_superfix', 'GALLERY', 'copper_valve', 'Valve work', 1),
    ('d_master_elec', 'HERO', 'led_bulbs', 'LED bulbs', 0),
    ('d_master_elec', 'GALLERY', 'switch_modern', 'Modular switch', 0),
    ('d_master_elec', 'GALLERY', 'switch_board', 'Switch board', 1),
    ('d_tilezone', 'HERO', 'tile_showroom', 'Tile showroom', 0),
    ('d_tilezone', 'HERO', 'tile_showroom2', 'Tile display racks', 1),
    ('d_tilezone', 'GALLERY', 'tile_showroom2', 'Sample boards', 0),
    ('d_trimurti', 'HERO', 'cement_shop', 'Cement and putty stock', 0),
    ('d_urbannest', 'HERO', 'fig_living_room', 'Living room display', 0),
    ('d_urbannest', 'HERO', 'fig_bedroom', 'Bedroom display', 1),
    ('d_urbannest', 'HERO', 'fig_carved_lounge', 'Carved lounge set', 2),
    ('d_urbannest', 'GALLERY', 'fig_modular_kitchen', 'Modular kitchen', 0),
    ('d_urbannest', 'GALLERY', 'fig_carved_lounge', 'Lounge seating', 1),
    ('d_urbannest', 'GALLERY', 'fig_bedroom', 'Bed and side tables', 2),
    ('d_lumina', 'HERO', 'fig_electrical_counter', 'Store counter', 0),
    ('d_lumina', 'HERO', 'fig_pendant_lights', 'Pendant lights', 1),
    ('d_lumina', 'GALLERY', 'led_bulbs', 'LED bulbs', 0),
    ('d_lumina', 'GALLERY', 'switch_modern', 'Modular switches', 1),
    ('d_lumina', 'GALLERY', 'switch_board', 'Switch boards', 2),
    ('d_stonecraft', 'HERO', 'fig_bath_showroom', 'Tile and bath showroom', 0),
    ('d_stonecraft', 'HERO', 'fig_marble_tiles', 'Marble tiles', 1),
    ('d_stonecraft', 'GALLERY', 'tile_showroom', 'Tile racks', 0),
    ('d_stonecraft', 'GALLERY', 'tile_showroom2', 'Sample boards', 1),
    ('d_aqualine', 'HERO', 'pvc_pipes', 'Pipes in stock', 0),
    ('d_aqualine', 'HERO', 'fig_bath_showroom', 'Sanitaryware display', 1),
    ('d_aqualine', 'GALLERY', 'copper_fittings', 'Fittings', 0),
    ('d_aqualine', 'GALLERY', 'pipe_fittings', 'Pipe fittings', 1)
) AS m(key, usage, img_key, alt, ord)
JOIN fx ON fx.key = m.key
JOIN img ON img.key = m.img_key;

-- ---------- workers (WORKER providers with their own contact listing) ----------
CREATE TEMP TABLE w_src (ord int, key text, name text, login_phone text, cat text, years int, availability text, bio text,
                         areas text[], services text[]) ON COMMIT DROP;
INSERT INTO w_src VALUES
    (1, 'w_manish', 'Manish Singh', NULL, 'painters', 11, 'AVAILABLE',
     'Manish is a local professional painter with 11 years of experience in residential painting, wall preparation, waterproofing and finishing work.',
     ARRAY['Lajpat Nagar', 'Ashok Vihar', 'Defence Colony', 'Saket', 'Mayur Vihar'],
     ARRAY['Interior Painting', 'Exterior Painting', 'Wall preparation', 'Waterproofing', 'Texture Painting', 'Installation Support']),
    (2, 'w_rakesh', 'Rakesh Kumar', NULL, 'carpenters', 12, 'AVAILABLE',
     'Rakesh builds and repairs modular furniture, wardrobes and doors, and has worked with interior contractors across West Delhi for 12 years.',
     ARRAY['Rajouri Garden', 'Janakpuri', 'Kirti Nagar'],
     ARRAY['Modular Furniture', 'Wardrobes', 'Door Fitting', 'Furniture Repair']),
    (3, 'w_anil', 'Anil Verma', NULL, 'electricians', 9, 'BUSY',
     'Anil is a licensed electrician handling house wiring, lighting, MCB panels and inverter installations.',
     ARRAY['Patel Nagar', 'Karol Bagh', 'Rajendra Nagar'],
     ARRAY['House Wiring', 'Fan & Light Installation', 'MCB & DB Repair', 'Inverter Setup']),
    (4, 'w_suresh', 'Suresh Pal', NULL, 'plumbers', 14, 'UNAVAILABLE',
     'Suresh has 14 years of plumbing experience in leak detection, bathroom fittings and water tank installation.',
     ARRAY['Laxmi Nagar', 'Preet Vihar', 'Mayur Vihar'],
     ARRAY['Leak Repair', 'Tap & Mixer Fitting', 'Drain Cleaning', 'Water Tank Installation']),
    -- added in a later release; each goes live once its login number is configured
    (5, 'w_ramesh', 'Ramesh Yadav', NULL, 'painters', 8, 'AVAILABLE',
     'Ramesh specialises in texture and designer wall finishes, and repaints flats quickly with minimal mess.',
     ARRAY['Rajouri Garden', 'Janakpuri', 'Tilak Nagar'],
     ARRAY['Texture Painting', 'Interior Painting', 'Wood Polish', 'Stencil Work']),
    (6, 'w_imran', 'Imran Qureshi', NULL, 'carpenters', 15, 'AVAILABLE',
     'Imran is a furniture maker with 15 years of experience in custom beds, wardrobes and solid-wood repairs.',
     ARRAY['Kirti Nagar', 'Moti Nagar', 'Patel Nagar'],
     ARRAY['Custom Furniture', 'Wardrobes', 'Furniture Polish', 'Furniture Repair']),
    (7, 'w_deepak', 'Deepak Sharma', NULL, 'electricians', 7, 'AVAILABLE',
     'Deepak installs smart switches, CCTV and home automation along with regular wiring and repairs.',
     ARRAY['Lajpat Nagar', 'Defence Colony', 'Greater Kailash'],
     ARRAY['Smart Home Setup', 'House Wiring', 'CCTV Installation', 'Fan & Light Installation']),
    (8, 'w_vikas', 'Vikas Gupta', NULL, 'plumbers', 10, 'BUSY',
     'Vikas handles bathroom renovations, concealed piping and geyser installation for homes and offices.',
     ARRAY['Paharganj', 'Karol Bagh', 'Connaught Place'],
     ARRAY['Bathroom Fitting', 'Concealed Piping', 'Geyser Installation', 'Leak Repair']),
    (9, 'w_mohan', 'Mohan Lal', NULL, 'masons', 18, 'AVAILABLE',
     'Mohan is a senior mason with 18 years of experience in brickwork, plastering and tile laying.',
     ARRAY['Okhla', 'Kalkaji', 'Govindpuri'],
     ARRAY['Brickwork', 'Plastering', 'Tile Laying', 'Waterproofing']),
    (10, 'w_sunil', 'Sunil Kumar', NULL, 'masons', 9, 'AVAILABLE',
     'Sunil takes on renovation and repair masonry: wall breaking, flooring, kitchen platforms and boundary walls.',
     ARRAY['Laxmi Nagar', 'Shahdara', 'Preet Vihar'],
     ARRAY['Renovation Work', 'Flooring', 'Kitchen Platforms', 'Plastering']),
    (11, 'w_arjun', 'Arjun Rawat', NULL, 'painters', 6, 'UNAVAILABLE',
     'Arjun paints homes and shops and offers waterproof exterior coatings before the monsoon.',
     ARRAY['Mayur Vihar', 'Patparganj', 'Laxmi Nagar'],
     ARRAY['Exterior Painting', 'Waterproofing', 'Interior Painting']),
    (12, 'w_sanjay', 'Sanjay Mishra', NULL, 'carpenters', 11, 'BUSY',
     'Sanjay builds modular kitchens and TV units and fits doors, windows and false-ceiling frames.',
     ARRAY['Saket', 'Malviya Nagar', 'Hauz Khas'],
     ARRAY['Modular Kitchens', 'TV Units', 'Door Fitting', 'Window Repair']),
    (13, 'w_rajesh', 'Rajesh Chauhan', NULL, 'electricians', 13, 'AVAILABLE',
     'Rajesh is a licensed electrician for commercial wiring, panel upgrades and inverter or solar connections.',
     ARRAY['Chandni Chowk', 'Civil Lines', 'Kamla Nagar'],
     ARRAY['Commercial Wiring', 'MCB & DB Repair', 'Inverter Setup', 'Earthing']),
    (14, 'w_prakash', 'Prakash Joshi', NULL, 'plumbers', 16, 'AVAILABLE',
     'Prakash has 16 years of plumbing experience in water tanks, motor pumps and full-house re-piping.',
     ARRAY['Dwarka', 'Uttam Nagar', 'Janakpuri'],
     ARRAY['Motor Pump Installation', 'Water Tank Installation', 'Re-piping', 'Drain Cleaning']);
UPDATE w_src SET login_phone = wp.phone FROM demo_worker_phone wp WHERE wp.ord = w_src.ord;
DELETE FROM w_src
WHERE login_phone IS NULL
   OR NOT EXISTS (SELECT 1 FROM city WHERE slug = 'new-delhi')
   OR EXISTS (SELECT 1 FROM users u WHERE u.email = w_src.key || '@fixtures.sfs.local')
   OR NOT EXISTS (SELECT 1 FROM category c WHERE c.slug = w_src.cat)
   OR EXISTS (SELECT 1 FROM users u WHERE u.phone_number = w_src.login_phone);

WITH u AS (
    INSERT INTO users (phone_number, role, email, name, onboarding_status, is_verified, created_at)
    SELECT login_phone, 'WORKER', key || '@fixtures.sfs.local', name, 'PROVIDER_READY', TRUE, now() FROM w_src
    RETURNING id, email
), b AS (
    INSERT INTO business (name, city_id, category_id, primary_phone, whatsapp_phone, is_active, website, timezone,
                          avg_rating, total_ratings, created_at, updated_at)
    SELECT w.name, s.city_id, (SELECT id FROM category WHERE slug = w.cat), s.phone, s.phone, TRUE,
           'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0, now(), now()
    FROM w_src w CROSS JOIN seed_ref s
    RETURNING id, name
), p AS (
    INSERT INTO provider_profile (user_id, business_id, provider_type, display_name, bio, primary_category_id,
                                  experience_years, verification_status, availability_status, availability_updated_at)
    SELECT (SELECT id FROM u WHERE email = w.key || '@fixtures.sfs.local'), (SELECT id FROM b WHERE name = w.name),
           'WORKER', w.name, w.bio, (SELECT id FROM category WHERE slug = w.cat),
           w.years, 'VERIFIED', w.availability, now()
    FROM w_src w
    RETURNING id, display_name
)
INSERT INTO fx (key, id) SELECT w.key, p.id FROM w_src w JOIN p ON p.display_name = w.name;

INSERT INTO provider_service_area (provider_id, city_id, locality)
SELECT fx.id, (SELECT city_id FROM seed_ref), a
FROM w_src w JOIN fx ON fx.key = w.key CROSS JOIN LATERAL unnest(w.areas) AS a;

INSERT INTO provider_service_offering (provider_id, name, sort_order)
SELECT fx.id, s.name, s.ord - 1
FROM w_src w JOIN fx ON fx.key = w.key CROSS JOIN LATERAL unnest(w.services) WITH ORDINALITY AS s(name, ord);

-- Typed charges: the first card on the worker page is the Visiting Charge (or the first charge).
INSERT INTO provider_rate (provider_id, rate_type, amount, currency, unit, note, sort_order)
SELECT fx.id, r.rate_type, r.amount, 'INR', r.unit, r.note, r.ord
FROM (VALUES
    ('w_manish', 'VISITING_CHARGE', 150.00, 'PER_VISIT', NULL, 0),
    ('w_manish', 'MATERIAL_COST', 18.50, 'PER_SQFT', 'Primer and two coats', 1),
    ('w_rakesh', 'MATERIAL_COST', 350.00, 'PER_SQFT', 'Laminated ply, fitted', 0),
    ('w_anil', 'SERVICE_FEE', 450.00, 'PER_JOB', NULL, 0),
    ('w_anil', 'HOURLY_RATE', 300.00, 'PER_HOUR', NULL, 1),
    ('w_suresh', 'VISITING_CHARGE', 200.00, 'PER_VISIT', NULL, 0),
    ('w_suresh', 'HOURLY_RATE', 300.00, 'PER_HOUR', NULL, 1),
    ('w_ramesh', 'VISITING_CHARGE', 100.00, 'PER_VISIT', NULL, 0),
    ('w_ramesh', 'MATERIAL_COST', 22.00, 'PER_SQFT', 'Texture finish', 1),
    ('w_imran', 'SERVICE_FEE', 500.00, 'PER_JOB', NULL, 0),
    ('w_imran', 'MATERIAL_COST', 420.00, 'PER_SQFT', 'Solid wood, polished', 1),
    ('w_deepak', 'VISITING_CHARGE', 199.00, 'PER_VISIT', NULL, 0),
    ('w_deepak', 'HOURLY_RATE', 350.00, 'PER_HOUR', NULL, 1),
    ('w_vikas', 'VISITING_CHARGE', 250.00, 'PER_VISIT', NULL, 0),
    ('w_mohan', 'SERVICE_FEE', 900.00, 'PER_JOB', 'Per day, one mason', 0),
    ('w_sunil', 'SERVICE_FEE', 800.00, 'PER_JOB', 'Per day, one mason', 0),
    ('w_arjun', 'VISITING_CHARGE', 120.00, 'PER_VISIT', NULL, 0),
    ('w_sanjay', 'MATERIAL_COST', 380.00, 'PER_SQFT', 'Laminated ply, fitted', 0),
    ('w_rajesh', 'SERVICE_FEE', 500.00, 'PER_JOB', NULL, 0),
    ('w_rajesh', 'HOURLY_RATE', 400.00, 'PER_HOUR', NULL, 1),
    ('w_prakash', 'VISITING_CHARGE', 200.00, 'PER_VISIT', NULL, 0)
) AS r(key, rate_type, amount, unit, note, ord)
JOIN fx ON fx.key = r.key;

-- ---------- dealer <-> worker connections ("Connected Worker" + "Recommended by") ----------
-- Dealers and workers are resolved whether they were created now or on an earlier start; a
-- connection is only added when the dealer or the worker is new, so links removed later in the
-- dashboard are not brought back.
CREATE TEMP TABLE dk ON COMMIT DROP AS
SELECT k.key, min(b.id) AS id
FROM (VALUES
    ('d_main', 'Sharma Paints & Hardware', TRUE),
    ('d_elite', 'Elite Home Solutions', TRUE),
    ('d_propaint', 'ProPaint Colour Studio', TRUE),
    ('d_hardware', 'Delhi Tools & Hardware Mart', TRUE),
    ('d_brightshine', 'Bright Shine Plumbing Services', FALSE),
    ('d_superfix', 'SuperFix Plumbing', FALSE),
    ('d_master_elec', 'Delhi Master Electricians', FALSE),
    ('d_tilezone', 'TileZone Studio', FALSE),
    ('d_trimurti', 'Trimurti Wall Care Products Pvt Ltd', FALSE),
    ('d_urbannest', 'Urban Nest Furniture Studio', TRUE),
    ('d_lumina', 'Lumina Electricals & Lighting', TRUE),
    ('d_stonecraft', 'StoneCraft Tiles & Bath Gallery', TRUE),
    ('d_aqualine', 'AquaLine Sanitary Hub', TRUE)
) AS k(key, name, demo)
JOIN business b ON b.name = k.name AND b.city_id = (SELECT city_id FROM seed_ref)
               AND (NOT k.demo OR b.website = 'https://fixtures.sfs.local/marketplace')
GROUP BY k.key;

CREATE TEMP TABLE wk ON COMMIT DROP AS
SELECT k.key, p.id
FROM (VALUES ('w_manish'), ('w_rakesh'), ('w_anil'), ('w_suresh'), ('w_ramesh'), ('w_imran'), ('w_deepak'),
             ('w_vikas'), ('w_mohan'), ('w_sunil'), ('w_arjun'), ('w_sanjay'), ('w_rajesh'), ('w_prakash')) AS k(key)
JOIN users u ON u.email = k.key || '@fixtures.sfs.local'
JOIN provider_profile p ON p.user_id = u.id;

-- A VERIFIED recommendation needs a reviewing dashboard user; without one it stays a plain connection.
INSERT INTO business_worker_link (business_id, provider_id, sort_order, recommendation_status, recommendation_note,
                                  recommendation_reviewed_by, recommendation_reviewed_at)
SELECT d.id, w.id, l.ord,
       CASE WHEN l.status = 'VERIFIED' AND s.reviewer_id IS NULL THEN 'NONE' ELSE l.status END,
       CASE WHEN l.status = 'VERIFIED' AND s.reviewer_id IS NULL THEN NULL ELSE l.note END,
       CASE WHEN l.status = 'VERIFIED' THEN s.reviewer_id END,
       CASE WHEN l.status = 'VERIFIED' AND s.reviewer_id IS NOT NULL THEN now() END
FROM (VALUES
    ('d_main', 'w_manish', 0, 'VERIFIED', 'This professional is recommended by the store based on their local referral network.'),
    ('d_main', 'w_rakesh', 1, 'VERIFIED', 'Fits all our modular hardware for customers; reliable and neat work.'),
    ('d_main', 'w_anil', 2, 'NONE', NULL),
    ('d_elite', 'w_manish', 0, 'VERIFIED', 'Has completed several painting jobs for our customers.'),
    ('d_propaint', 'w_rakesh', 0, 'NONE', NULL),
    ('d_brightshine', 'w_suresh', 0, 'VERIFIED', 'Our senior plumber for bathroom and re-piping jobs.'),
    ('d_master_elec', 'w_anil', 0, 'VERIFIED', 'Certified electrician we send for all wiring jobs.'),
    ('d_tilezone', 'w_rakesh', 0, 'NONE', NULL),
    ('d_urbannest', 'w_imran', 0, 'VERIFIED', 'Makes and repairs our custom furniture orders.'),
    ('d_urbannest', 'w_sanjay', 1, 'VERIFIED', 'Installs our modular kitchens and TV units.'),
    ('d_urbannest', 'w_rakesh', 2, 'NONE', NULL),
    ('d_lumina', 'w_deepak', 0, 'VERIFIED', 'Our go-to electrician for smart-home and lighting installs.'),
    ('d_lumina', 'w_rajesh', 1, 'VERIFIED', 'Handles commercial wiring jobs for our bulk customers.'),
    ('d_lumina', 'w_anil', 2, 'NONE', NULL),
    ('d_stonecraft', 'w_mohan', 0, 'VERIFIED', 'Lays tiles and marble for our customers; careful, level work.'),
    ('d_stonecraft', 'w_vikas', 1, 'VERIFIED', 'Fits our bath ranges, including concealed piping.'),
    ('d_aqualine', 'w_prakash', 0, 'VERIFIED', 'Senior plumber we send for tank and pump installations.'),
    ('d_aqualine', 'w_vikas', 1, 'NONE', NULL),
    ('d_trimurti', 'w_sunil', 0, 'VERIFIED', 'Buys putty and cement from us for renovation jobs.'),
    ('d_main', 'w_ramesh', 3, 'VERIFIED', 'Our customers ask for him for texture finishes.'),
    ('d_propaint', 'w_arjun', 1, 'NONE', NULL),
    ('d_master_elec', 'w_rajesh', 1, 'NONE', NULL),
    ('d_superfix', 'w_prakash', 0, 'NONE', NULL)
) AS l(dkey, wkey, ord, status, note)
JOIN dk d ON d.key = l.dkey
JOIN wk w ON w.key = l.wkey
CROSS JOIN seed_ref s
WHERE (EXISTS (SELECT 1 FROM fx WHERE fx.key = l.dkey) OR EXISTS (SELECT 1 FROM fx WHERE fx.key = l.wkey))
ON CONFLICT (business_id, provider_id) DO NOTHING;

-- ---------- contact number top-up (demo stores/workers created before a number was set) ----------
CREATE TEMP TABLE seed_contacts ON COMMIT DROP AS SELECT 0 AS n;
WITH upd AS (
    UPDATE business b SET primary_phone = s.phone, whatsapp_phone = s.phone, updated_at = now()
    FROM seed_ref s
    WHERE b.website = 'https://fixtures.sfs.local/marketplace' AND s.phone IS NOT NULL
      AND b.primary_phone IS NULL AND b.whatsapp_phone IS NULL
    RETURNING 1
)
UPDATE seed_contacts SET n = (SELECT count(*) FROM upd);

-- ---------- demo photo top-up (workers and stores that have none) ----------
-- Home Services cards and worker pages need a photo, and a demo store may have lost all of its own.
-- Runs on every start, only for demo rows (stores tagged with the fixtures website, and the demo
-- workers' own listings), only where there is no photo yet, so photos added in the dashboard are
-- never replaced or duplicated. The photos are the Figma "Spotlight" images bundled with the
-- backend (src/main/resources/demo-images) and served at /api/public/demo-images/. Skipped when no
-- image base URL is configured.
CREATE TEMP TABLE seed_photos ON COMMIT DROP AS SELECT 0 AS n;
WITH worker_photo AS (
    INSERT INTO provider_media (provider_id, media_type, url, sort_order, created_at, updated_at)
    SELECT p.id, 'GALLERY',
           (SELECT image_base FROM demo_cfg) || '/api/public/demo-images/' ||
           CASE c.slug WHEN 'painters' THEN 'worker-painters.jpg'
                       WHEN 'electricians' THEN 'worker-electricians.jpg'
                       WHEN 'plumbers' THEN 'worker-plumbers.jpg'
                       WHEN 'carpenters' THEN 'worker-carpenters.jpg'
                       -- no mason photo in the Figma set; the cement and building-materials shop is closest
                       WHEN 'masons' THEN 'marketplace/cement-shop.jpg'
                       ELSE 'worker-' || (ARRAY['painters', 'electricians', 'plumbers', 'carpenters'])[1 + (p.id % 4)::int] || '.jpg' END,
           0, now(), now()
    FROM provider_profile p
    JOIN business b ON b.id = p.business_id
    JOIN category c ON c.id = p.primary_category_id
    WHERE p.provider_type = 'WORKER'
      AND b.website = 'https://fixtures.sfs.local/marketplace'
      AND (SELECT image_base FROM demo_cfg) IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM provider_media m WHERE m.provider_id = p.id AND m.media_type = 'GALLERY')
    RETURNING 1
), store_photo AS (
    INSERT INTO business_media (business_id, usage_type, media_url, alt_text, sort_order)
    SELECT b.id, 'HERO',
           (SELECT image_base FROM demo_cfg) || '/api/public/demo-images/store-' || (1 + (b.id % 4)::int) || '.jpg',
           b.name, 0
    FROM business b
    WHERE b.website = 'https://fixtures.sfs.local/marketplace'
      AND (SELECT image_base FROM demo_cfg) IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM provider_profile wp WHERE wp.business_id = b.id AND wp.provider_type = 'WORKER')
      AND NOT EXISTS (SELECT 1 FROM business_media m WHERE m.business_id = b.id)
    RETURNING 1
)
UPDATE seed_photos SET n = (SELECT count(*) FROM worker_photo) + (SELECT count(*) FROM store_photo);

-- Photos stored with a hot-link on an earlier start are repointed to the bundled copy. Only exact
-- hot-link URLs match, so a photo replaced in the dashboard is never touched.
WITH moved AS (
    UPDATE business_media bm SET media_url = m.local_url
    FROM img_move m
    WHERE bm.media_url = m.hotlink
    RETURNING 1
)
UPDATE seed_photos SET n = n + (SELECT count(*) FROM moved);
