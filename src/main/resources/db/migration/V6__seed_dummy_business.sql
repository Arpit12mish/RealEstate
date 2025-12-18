-- V6__seed_dummy_business.sql
-- Seed 10 realistic dummy businesses mapped to the new subcategories.
-- NOTE: all businesses use city_id = 1 (e.g., Delhi). Make sure city with id=1 exists.

-- 1) Bright Shine Plumbing Services (Plumbers)
INSERT INTO business (
    name, primary_phone, email, address_line1, landmark, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'Bright Shine Plumbing Services', '9811122233', 'contact@brightshine.in',
    'C-55, Laxmi Nagar', 'Near Metro Station', '110092',
    1, 28.6325, 77.2772,
    c.id, 4.5, 120
FROM category c
WHERE c.slug = 'plumbers';


-- 2) Delhi Master Electricians (Electricians)
INSERT INTO business (
    name, primary_phone, email, address_line1, landmark, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'Delhi Master Electricians', '9812233445', 'support@dmelectric.in',
    'Shop 22, Patel Nagar', 'Opp. Pillar No. 223', '110008',
    1, 28.6510, 77.1280,
    c.id, 4.2, 90
FROM category c
WHERE c.slug = 'electricians';


-- 3) HomeCraft Interiors (Interior Designers)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'HomeCraft Interiors', '9899776655', 'hello@homecraft.in',
    'A-121, Green Park', '110016',
    1, 28.5590, 77.2050,
    c.id, 4.8, 220
FROM category c
WHERE c.slug = 'interior-designers';


-- 4) Urban Edge Architects (Architects)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'Urban Edge Architects', '9876543210', 'info@urbanedge.in',
    'D-19, South Ex - Part 1', '110049',
    1, 28.5699, 77.2090,
    c.id, 4.6, 150
FROM category c
WHERE c.slug = 'architects';


-- 5) TileZone Studio (Tiles & Flooring)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'TileZone Studio', '9812345678', 'sales@tilezone.in',
    'Ground Floor, Kirti Nagar', '110015',
    1, 28.6518, 77.1250,
    c.id, 4.1, 85
FROM category c
WHERE c.slug = 'tiles-flooring';


-- 6) SafeHome Digital Locks (Digital Locks & Safes)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'SafeHome Digital Locks', '9821223344', 'support@safehome.in',
    '14, Rani Bagh Market', '110034',
    1, 28.7040, 77.1320,
    c.id, 4.4, 110
FROM category c
WHERE c.slug = 'digital-locks-safes';


-- 7) SuperFix Plumbing (Plumbers)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'SuperFix Plumbing', '9820034567', 'help@superfixplumbers.in',
    'Shop 31, Andheri East (Demo, still city 1)', '110070',
    1, 28.6100, 77.1000,
    c.id, 4.3, 95
FROM category c
WHERE c.slug = 'plumbers';


-- 8) WestEnd Interiors (Interior Designers)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'WestEnd Interiors', '9833111222', 'design@westend.in',
    '602, Lokhandwala (Demo, still city 1)', '110071',
    1, 28.6200, 77.1100,
    c.id, 4.7, 140
FROM category c
WHERE c.slug = 'interior-designers';


-- 9) ProFix Home Services (Appliances Repair)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'ProFix Home Services', '9870011223', 'contact@profix.in',
    'Sector 9, Dwarka', '110077',
    1, 28.5920, 77.0460,
    c.id, 4.5, 210
FROM category c
WHERE c.slug = 'appliances-repair';


-- 10) AllCity Movers & Packers (Movers & Packers)
INSERT INTO business (
    name, primary_phone, email, address_line1, pincode,
    city_id, latitude, longitude, category_id, avg_rating, total_ratings
)
SELECT
    'AllCity Movers & Packers', '9844332211', 'info@allcity.in',
    'Plot 55, Janakpuri', '110058',
    1, 28.6210, 77.0860,
    c.id, 4.0, 75
FROM category c
WHERE c.slug = 'movers-packers';
