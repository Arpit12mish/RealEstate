-- LOCAL DEV ONLY: Marketplace dealer + Allied Services worker demo data for New Delhi.
-- Fills every section of the dealer screen (Figma 2995:1723) and worker screen (Figma 3054:5700):
-- hero photos, about, products, services, store photos, similar stores, connected workers,
-- reviews, worker charges, services, service areas and "Recommended by".
--
-- Refuses to run anywhere except a local database (sfs_db_local / sfs_local_staging).
-- Re-runnable: rows it creates are tagged (business.website / users.email) and removed first.
-- It also decorates five existing local New Delhi businesses (matched by id AND name); on every
-- run their photos, hours, products/services and connected workers are replaced.
--
-- Run (from the RealEstate folder):
--   psql -h localhost -U postgres -d sfs_db_local -v ON_ERROR_STOP=1 -f infra/local-staging/fixtures/marketplace-demo-delhi.sql
--
-- Photos are openly licensed (Wikimedia Commons / Flickr via Openverse), hot-linked for local testing only:
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
DO $$
BEGIN
    IF current_database() NOT IN ('sfs_db_local', 'sfs_local_staging') THEN
        RAISE EXCEPTION 'marketplace demo data may only be loaded into a local database (got %)', current_database();
    END IF;
END $$;

BEGIN;

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

CREATE TEMP TABLE fx (key text PRIMARY KEY, id bigint NOT NULL) ON COMMIT DROP;

-- ---------- existing local businesses to decorate (only if id and name still match) ----------
INSERT INTO fx (key, id)
SELECT e.key, b.id
FROM (VALUES
    ('d_brightshine', 3, 'Bright Shine Plumbing Services'),
    ('d_superfix', 9, 'SuperFix Plumbing'),
    ('d_master_elec', 4, 'Delhi Master Electricians'),
    ('d_tilezone', 7, 'TileZone Studio'),
    ('d_trimurti', 13, 'Trimurti Wall Care Products Pvt Ltd')
) AS e(key, id, name)
JOIN business b ON b.id = e.id AND b.name = e.name;

-- ---------- cleanup of a previous load ----------
DELETE FROM business_review WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@fixtures.sfs.local');
DELETE FROM business_worker_link WHERE business_id IN (SELECT id FROM fx);
DELETE FROM business_media WHERE business_id IN (SELECT id FROM fx);
DELETE FROM business_opening_hours WHERE business_id IN (SELECT id FROM fx);
DELETE FROM business_offering_group WHERE business_id IN (SELECT id FROM fx);
-- Some local databases still have non-cascading FKs to provider_profile / business.
DELETE FROM provider_service_area WHERE provider_id IN
    (SELECT id FROM provider_profile WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@fixtures.sfs.local'));
DELETE FROM provider_project WHERE provider_id IN
    (SELECT id FROM provider_profile WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@fixtures.sfs.local'));
DELETE FROM provider_profile WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@fixtures.sfs.local');
DELETE FROM business_event WHERE business_id IN (SELECT id FROM business WHERE website = 'https://fixtures.sfs.local/marketplace');
DELETE FROM business WHERE website = 'https://fixtures.sfs.local/marketplace';
DELETE FROM users WHERE email LIKE '%@fixtures.sfs.local';

-- ---------- new dealers ----------
WITH rows(key, name, cat, locality, landmark, phone, est, prio, descr) AS (VALUES
    ('d_main', 'Sharma Paints & Hardware', 'paint-finishes', 'Lajpat Nagar', 'Near Central Market', '+919900000201', 2008, 99,
     'A local paint and hardware supplier serving homeowners, contractors, and interior professionals with paints, waterproofing solutions, hardware, and related building products.'),
    ('d_elite', 'Elite Home Solutions', 'paint-finishes', 'Rajouri Garden', 'Opp. Metro Pillar 412', '+919900000202', 2012, 95,
     'Decorative paints, sprayers and waterproof coatings for homes and offices, with on-site colour consultation.'),
    ('d_propaint', 'ProPaint Colour Studio', 'paint-finishes', 'Karol Bagh', 'Ajmal Khan Road', '+919900000203', 2015, 93,
     'Colour studio with computerised tinting, rollers, brushes and wood finishes for painters and DIY customers.'),
    ('d_hardware', 'Delhi Tools & Hardware Mart', 'hardware-tools', 'Chandni Chowk', 'Bhagirath Palace', '+919900000204', 1996, 91,
     'One of Old Delhi''s busiest hardware counters: power tools, hand tools, fasteners, locks and door fittings in retail and bulk.')
), ins AS (
    INSERT INTO business (name, city_id, category_id, locality, landmark, primary_phone, whatsapp_phone, established_year,
                          is_active, description, website, timezone, avg_rating, total_ratings,
                          sponsored, sponsored_priority, sponsored_start, sponsored_end, created_at, updated_at)
    SELECT r.name, (SELECT id FROM city WHERE slug = 'new-delhi'), (SELECT id FROM category WHERE slug = r.cat),
           r.locality, r.landmark, r.phone, r.phone, r.est, TRUE, r.descr,
           'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0,
           TRUE, r.prio, now() - interval '1 day', now() + interval '1 year', now(), now()
    FROM rows r
    RETURNING id, name
)
INSERT INTO fx (key, id) SELECT r.key, ins.id FROM rows r JOIN ins ON ins.name = r.name;

-- ---------- decorate the existing businesses ----------
UPDATE business b SET
    locality = v.locality,
    description = v.descr,
    established_year = COALESCE(b.established_year, v.est),
    whatsapp_phone = COALESCE(b.whatsapp_phone, b.primary_phone),
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
    ('d_trimurti', 1, 6, '09:00', '18:30')
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
    ('d_trimurti', 'PRODUCT', 'Building Materials', 0, ARRAY['Wall Putty', 'Cement', 'White Cement', 'Tile Adhesive']);

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
    ('d_trimurti', 'HERO', 'cement_shop', 'Cement and putty stock', 0)
) AS m(key, usage, img_key, alt, ord)
JOIN fx ON fx.key = m.key
JOIN img ON img.key = m.img_key;

-- ---------- workers (WORKER providers with their own contact listing) ----------
CREATE TEMP TABLE w_src (key text, name text, phone text, cat text, years int, availability text, bio text,
                         areas text[], services text[]) ON COMMIT DROP;
INSERT INTO w_src VALUES
    ('w_manish', 'Manish Singh', '+919910000101', 'painters', 11, 'AVAILABLE',
     'Manish is a local professional painter with 11 years of experience in residential painting, wall preparation, waterproofing and finishing work.',
     ARRAY['Lajpat Nagar', 'Ashok Vihar', 'Defence Colony', 'Saket', 'Mayur Vihar'],
     ARRAY['Interior Painting', 'Exterior Painting', 'Wall preparation', 'Waterproofing', 'Texture Painting', 'Installation Support']),
    ('w_rakesh', 'Rakesh Kumar', '+919910000102', 'carpenters', 12, 'AVAILABLE',
     'Rakesh builds and repairs modular furniture, wardrobes and doors, and has worked with interior contractors across West Delhi for 12 years.',
     ARRAY['Rajouri Garden', 'Janakpuri', 'Kirti Nagar'],
     ARRAY['Modular Furniture', 'Wardrobes', 'Door Fitting', 'Furniture Repair']),
    ('w_anil', 'Anil Verma', '+919910000103', 'electricians', 9, 'BUSY',
     'Anil is a licensed electrician handling house wiring, lighting, MCB panels and inverter installations.',
     ARRAY['Patel Nagar', 'Karol Bagh', 'Rajendra Nagar'],
     ARRAY['House Wiring', 'Fan & Light Installation', 'MCB & DB Repair', 'Inverter Setup']),
    ('w_suresh', 'Suresh Pal', '+919910000104', 'plumbers', 14, 'UNAVAILABLE',
     'Suresh has 14 years of plumbing experience in leak detection, bathroom fittings and water tank installation.',
     ARRAY['Laxmi Nagar', 'Preet Vihar', 'Mayur Vihar'],
     ARRAY['Leak Repair', 'Tap & Mixer Fitting', 'Drain Cleaning', 'Water Tank Installation']);

WITH u AS (
    INSERT INTO users (phone_number, role, email, name, onboarding_status, is_verified, created_at)
    SELECT phone, 'WORKER', key || '@fixtures.sfs.local', name, 'PROVIDER_READY', TRUE, now() FROM w_src
    RETURNING id, email
), b AS (
    INSERT INTO business (name, city_id, category_id, primary_phone, whatsapp_phone, is_active, website, timezone,
                          avg_rating, total_ratings, created_at, updated_at)
    SELECT w.name, (SELECT id FROM city WHERE slug = 'new-delhi'), (SELECT id FROM category WHERE slug = w.cat),
           w.phone, w.phone, TRUE, 'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0, now(), now()
    FROM w_src w
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
SELECT fx.id, (SELECT id FROM city WHERE slug = 'new-delhi'), a
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
    ('w_suresh', 'HOURLY_RATE', 300.00, 'PER_HOUR', NULL, 1)
) AS r(key, rate_type, amount, unit, note, ord)
JOIN fx ON fx.key = r.key;

-- ---------- dealer <-> worker connections ("Connected Worker" + verified "Recommended by") ----------
INSERT INTO business_worker_link (business_id, provider_id, sort_order, recommendation_status, recommendation_note,
                                  recommendation_reviewed_by, recommendation_reviewed_at)
SELECT d.id, w.id, l.ord, l.status, l.note,
       CASE WHEN l.status = 'VERIFIED' THEN (SELECT min(id) FROM dashboard_users) END,
       CASE WHEN l.status = 'VERIFIED' THEN now() END
FROM (VALUES
    ('d_main', 'w_manish', 0, 'VERIFIED', 'This professional is recommended by the store based on their local referral network.'),
    ('d_main', 'w_rakesh', 1, 'VERIFIED', 'Fits all our modular hardware for customers; reliable and neat work.'),
    ('d_main', 'w_anil', 2, 'NONE', NULL),
    ('d_elite', 'w_manish', 0, 'VERIFIED', 'Has completed several painting jobs for our customers.'),
    ('d_propaint', 'w_rakesh', 0, 'NONE', NULL),
    ('d_brightshine', 'w_suresh', 0, 'VERIFIED', 'Our senior plumber for bathroom and re-piping jobs.'),
    ('d_master_elec', 'w_anil', 0, 'VERIFIED', 'Certified electrician we send for all wiring jobs.'),
    ('d_tilezone', 'w_rakesh', 0, 'NONE', NULL)
) AS l(dkey, wkey, ord, status, note)
JOIN fx d ON d.key = l.dkey
JOIN fx w ON w.key = l.wkey;

-- ---------- approved reviews from demo customers ----------
INSERT INTO users (phone_number, role, email, name, is_verified, created_at)
SELECT phone, 'CUSTOMER', key || '@fixtures.sfs.local', name, TRUE, now()
FROM (VALUES
    ('r_rahul', '+919910000191', 'Rahul Mehta'),
    ('r_amit', '+919910000192', 'Amit Khanna'),
    ('r_priya', '+919910000193', 'Priya Sharma'),
    ('r_neha', '+919910000194', 'Neha Arora'),
    ('r_vikram', '+919910000195', 'Vikram Malhotra')
) AS r(key, phone, name);

INSERT INTO business_review (business_id, user_id, reviewer_name, reviewer_location, rating, review_text,
                             moderation_status, moderated_by_dashboard_user_id, moderated_at, created_at)
SELECT fx.id, u.id, u.name, rv.loc, rv.rating, rv.txt, 'APPROVED', (SELECT min(id) FROM dashboard_users),
       now() - (rv.days || ' days')::interval, now() - (rv.days || ' days')::interval
FROM (VALUES
    ('d_main', 'r_rahul', 'Lajpat Nagar', 5, 'Premium paint quality and a smooth buying experience. They matched the exact shade from a photo and delivered the same evening.', 3),
    ('d_main', 'r_amit', 'Defence Colony', 4, 'Excellent range of waterproofing products. Slightly busy on weekends, but the staff explained every option patiently.', 9),
    ('d_main', 'r_priya', 'Greater Kailash', 5, 'Got paints, putty and door hardware in one place. Their recommended painter Manish did a very neat job.', 21),
    ('d_elite', 'r_neha', 'Rajouri Garden', 4, 'Good texture paint options and helpful colour consultation.', 12),
    ('d_propaint', 'r_vikram', 'Karol Bagh', 4, 'Fast computerised tinting and fair prices on rollers and brushes.', 15),
    ('d_hardware', 'r_rahul', 'Chandni Chowk', 5, 'Everything from screws to power tools at wholesale rates.', 30),
    ('d_brightshine', 'r_amit', 'Laxmi Nagar', 5, 'Fixed a hidden bathroom leak within an hour of calling. Very professional.', 5),
    ('d_brightshine', 'r_neha', 'Preet Vihar', 4, 'Good work on our CPVC re-piping; arrived on time.', 18),
    ('d_master_elec', 'r_priya', 'Patel Nagar', 5, 'Rewired our flat neatly and explained the new MCB panel.', 7),
    ('d_tilezone', 'r_vikram', 'Kirti Nagar', 4, 'Huge tile collection and the layout design service was useful.', 25)
) AS rv(dkey, ukey, loc, rating, txt, days)
JOIN fx ON fx.key = rv.dkey
JOIN users u ON u.email = rv.ukey || '@fixtures.sfs.local';

-- Keep the rating summary consistent with approved reviews.
UPDATE business b SET avg_rating = s.avg, total_ratings = s.cnt
FROM (SELECT business_id, round(avg(rating)::numeric, 1) AS avg, count(*) AS cnt
      FROM business_review WHERE moderation_status = 'APPROVED' GROUP BY business_id) s
WHERE b.id = s.business_id AND b.id IN (SELECT id FROM fx);

SELECT key, id FROM fx ORDER BY key;

COMMIT;
