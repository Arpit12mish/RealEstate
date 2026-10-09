-- LOCAL-STAGING ONLY demo data for the Marketplace dealer / Allied Services worker detail screens.
-- Refuses to run anywhere except the local-staging database. Idempotent: every row it creates is
-- tagged (business.website / users.email) and removed before re-inserting.
--
-- Run:
--   docker compose --env-file infra/local-staging/.env -f infra/local-staging/docker-compose.yml \
--     exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
--     < infra/local-staging/fixtures/marketplace-details-demo.sql

DO $$
BEGIN
    IF current_database() <> 'sfs_local_staging' THEN
        RAISE EXCEPTION 'marketplace demo fixtures may only be loaded into sfs_local_staging (got %)', current_database();
    END IF;
END $$;

BEGIN;

-- ---------- cleanup of a previous load ----------
DELETE FROM provider_profile
WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@fixtures.sfs.local');
DELETE FROM business WHERE website = 'https://fixtures.sfs.local/marketplace';
DELETE FROM users WHERE email LIKE '%@fixtures.sfs.local';

CREATE TEMP TABLE fx (key text PRIMARY KEY, id bigint) ON COMMIT DROP;

-- ---------- dealers ----------
WITH city AS (SELECT id FROM city WHERE slug = 'gurugram'),
     paints AS (SELECT id FROM category WHERE slug = 'paints-finishes'),
     hardware AS (SELECT id FROM category WHERE slug = 'hardware-tools'),
     rows(key, name, cat, locality, phone, whatsapp, est, active, descr) AS (VALUES
        ('d_main', 'Gupta Colour House', 'paints', 'Sector 26', '+919900000101', '+919900000101', 2008, TRUE,
         'A neighbourhood paint and hardware supplier serving homeowners, contractors and interior professionals with paints, waterproofing solutions, hardware and related building products.'),
        ('d_sim1', 'Kapoor Paint Studio', 'paints', 'Sector 14', '01244567890', NULL, 2014, TRUE, 'Decorative paints and texture finishes.'),
        ('d_sim2', 'Malhotra Hardware Mart', 'paints', 'DLF Phase 2', '+919900000103', '+919900000103', 2001, TRUE, 'Paints, primers and painting tools.'),
        ('d_sim3', 'Verma Colour Point', 'paints', 'Sohna Road', NULL, NULL, 2019, TRUE, 'Newly opened store - contact details pending.'),
        ('d_hidden', 'Archived Paints (inactive)', 'paints', 'Sector 31', '+919900000105', NULL, 2010, FALSE, 'Inactive listing; must never appear.'),
        ('d_other', 'Bansal Tools', 'hardware', 'Sector 29', '+919900000106', NULL, 2016, TRUE, 'Different category; not similar.')
     ),
     ins AS (
        INSERT INTO business (name, city_id, category_id, locality, primary_phone, whatsapp_phone, established_year,
                              is_active, description, website, timezone, avg_rating, total_ratings)
        SELECT r.name, (SELECT id FROM city),
               CASE r.cat WHEN 'paints' THEN (SELECT id FROM paints) ELSE (SELECT id FROM hardware) END,
               r.locality, r.phone, r.whatsapp, r.est, r.active, r.descr,
               'https://fixtures.sfs.local/marketplace', 'Asia/Kolkata', 0, 0
        FROM rows r
        RETURNING id, name
     )
INSERT INTO fx (key, id)
SELECT r.key, ins.id FROM rows r JOIN ins ON ins.name = r.name;

-- Opening hours: main store Mon-Sat 09:00-22:00 (closed Sunday); sim1 has an overnight Friday.
INSERT INTO business_opening_hours (business_id, day_of_week, opens_at, closes_at)
SELECT (SELECT id FROM fx WHERE key = 'd_main'), d, '09:00', '22:00' FROM generate_series(1, 6) d;
INSERT INTO business_opening_hours (business_id, day_of_week, opens_at, closes_at)
SELECT (SELECT id FROM fx WHERE key = 'd_sim1'), d, '10:00', '19:45' FROM generate_series(1, 4) d;
INSERT INTO business_opening_hours (business_id, day_of_week, opens_at, closes_at) VALUES
    ((SELECT id FROM fx WHERE key = 'd_sim1'), 5, '10:00', '01:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 1, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 2, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 3, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 4, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 5, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 6, '00:00', '00:00'),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 7, '00:00', '00:00');

-- Products and services
WITH g AS (
    INSERT INTO business_offering_group (business_id, offering_type, title, sort_order) VALUES
        ((SELECT id FROM fx WHERE key = 'd_main'), 'PRODUCT', 'Paints', 0),
        ((SELECT id FROM fx WHERE key = 'd_main'), 'PRODUCT', 'Hardware', 1),
        ((SELECT id FROM fx WHERE key = 'd_main'), 'SERVICE', 'Services', 0),
        ((SELECT id FROM fx WHERE key = 'd_sim1'), 'PRODUCT', 'Finishes', 0),
        ((SELECT id FROM fx WHERE key = 'd_sim2'), 'PRODUCT', 'Paints', 0)
    RETURNING id, business_id, offering_type, title
)
INSERT INTO business_offering_item (group_id, name, sort_order)
SELECT g.id, item.name, item.ord - 1
FROM g
JOIN LATERAL unnest(CASE
    WHEN g.title = 'Paints' AND g.business_id = (SELECT id FROM fx WHERE key = 'd_main')
        THEN ARRAY['Interior Paint', 'Exterior Paint', 'Wood Paint', 'Metal Paint', 'Wall Putty', 'Primers']
    WHEN g.title = 'Hardware' THEN ARRAY['Door Hardware', 'Locks', 'Fasteners', 'Hand Tools']
    WHEN g.title = 'Services' THEN ARRAY['Color Consultation', 'Paint Matching', 'Home Delivery', 'Contractor Support', 'Bulk Orders', 'Installation Support']
    WHEN g.title = 'Finishes' THEN ARRAY['Texture Coats', 'Sealants', 'Varnishes']
    ELSE ARRAY['Waterproofing', 'Wall Putty', 'Primers', 'Rollers']
END) WITH ORDINALITY AS item(name, ord) ON TRUE;

-- Media (stable public sample photos; https is enforced by the schema)
INSERT INTO business_media (business_id, usage_type, media_url, alt_text, sort_order)
SELECT (SELECT id FROM fx WHERE key = 'd_main'), u, 'https://picsum.photos/id/' || pid || '/1200/800', alt, ord
FROM (VALUES
    ('HERO', 1062, 'Store front', 0), ('HERO', 1060, 'Paint aisle', 1), ('HERO', 1059, 'Colour counter', 2), ('HERO', 1058, 'Tools wall', 3),
    ('GALLERY', 1050, NULL, 0), ('GALLERY', 1051, NULL, 1), ('GALLERY', 1052, NULL, 2), ('GALLERY', 1053, NULL, 3),
    ('GALLERY', 1054, NULL, 4), ('GALLERY', 1055, NULL, 5)
) AS m(u, pid, alt, ord);
INSERT INTO business_media (business_id, usage_type, media_url, sort_order) VALUES
    ((SELECT id FROM fx WHERE key = 'd_sim1'), 'HERO', 'https://picsum.photos/id/1040/800/600', 0),
    ((SELECT id FROM fx WHERE key = 'd_sim1'), 'GALLERY', 'https://picsum.photos/id/1041/800/600', 0),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), 'HERO', 'https://picsum.photos/id/1043/800/600', 0);

-- ---------- workers (WORKER providers with their own linked listing) ----------
WITH city AS (SELECT id FROM city WHERE slug = 'gurugram'),
     rows(key, name, phone, cat, years, verification, availability, bio) AS (VALUES
        ('w_paint', 'Rohit Yadav', '+919910000001', 'painters', 11, 'VERIFIED', 'AVAILABLE',
         'Rohit is a local professional painter with 11 years of experience in residential painting, wall preparation, waterproofing and finishing work.'),
        ('w_carp', 'Sunil Rawat', '+919910000002', 'carpenters', 8, 'VERIFIED', 'BUSY', 'Modular furniture and door fitting specialist.'),
        ('w_elec', 'Imran Qureshi', '+919910000003', 'electricians', 14, 'VERIFIED', 'UNAVAILABLE', 'Licensed electrician for wiring and fittings.'),
        ('w_pending', 'Pending Verification Worker', '+919910000004', 'plumbers', 3, 'PENDING', 'AVAILABLE', 'Not yet verified; must stay hidden.')
     ),
     u AS (
        INSERT INTO users (phone_number, role, email, name, onboarding_status)
        SELECT phone, 'WORKER', key || '@fixtures.sfs.local', name, 'PROVIDER_READY' FROM rows
        RETURNING id, email
     ),
     b AS (
        INSERT INTO business (name, city_id, category_id, primary_phone, whatsapp_phone, is_active, website, avg_rating, total_ratings)
        SELECT r.name, (SELECT id FROM city), (SELECT id FROM category WHERE slug = r.cat), r.phone, r.phone, TRUE,
               'https://fixtures.sfs.local/marketplace', 0, 0
        FROM rows r
        RETURNING id, name
     ),
     p AS (
        INSERT INTO provider_profile (user_id, business_id, provider_type, display_name, bio, primary_category_id,
                                      experience_years, verification_status, availability_status, availability_updated_at)
        SELECT (SELECT id FROM u WHERE email = r.key || '@fixtures.sfs.local'),
               (SELECT id FROM b WHERE name = r.name),
               'WORKER', r.name, r.bio, (SELECT id FROM category WHERE slug = r.cat),
               r.years, r.verification, r.availability, now()
        FROM rows r
        RETURNING id, display_name
     )
INSERT INTO fx (key, id)
SELECT r.key, p.id FROM rows r JOIN p ON p.display_name = r.name;

INSERT INTO provider_service_area (provider_id, city_id, locality)
SELECT (SELECT id FROM fx WHERE key = 'w_paint'), (SELECT id FROM city WHERE slug = 'gurugram'), loc
FROM unnest(ARRAY['Sector 14', 'Ashok Vihar', 'Sector 16', 'Sector 18', 'Mayur Vihar']) loc;
INSERT INTO provider_service_area (provider_id, city_id, locality) VALUES
    ((SELECT id FROM fx WHERE key = 'w_carp'), (SELECT id FROM city WHERE slug = 'gurugram'), 'DLF Phase 3'),
    ((SELECT id FROM fx WHERE key = 'w_elec'), (SELECT id FROM city WHERE slug = 'gurugram'), NULL);

INSERT INTO provider_service_offering (provider_id, name, sort_order)
SELECT (SELECT id FROM fx WHERE key = 'w_paint'), s, ord - 1
FROM unnest(ARRAY['Interior Painting', 'Exterior Painting', 'Wall preparation', 'Waterproofing', 'Texture Painting', 'Installation Support'])
     WITH ORDINALITY AS t(s, ord);

-- Typed charges: each card shows the charge's own label.
INSERT INTO provider_rate (provider_id, rate_type, amount, currency, unit, note, sort_order) VALUES
    ((SELECT id FROM fx WHERE key = 'w_paint'), 'VISITING_CHARGE', 150.00, 'INR', 'PER_VISIT', NULL, 0),
    ((SELECT id FROM fx WHERE key = 'w_paint'), 'MATERIAL_COST', 18.50, 'INR', 'PER_SQFT', 'Primer and two coats', 1),
    ((SELECT id FROM fx WHERE key = 'w_carp'), 'SERVICE_FEE', 450.00, 'INR', 'PER_JOB', NULL, 0),
    ((SELECT id FROM fx WHERE key = 'w_elec'), 'HOURLY_RATE', 300.00, 'INR', 'PER_HOUR', NULL, 0);

-- ---------- dealer <-> worker links ----------
INSERT INTO business_worker_link (business_id, provider_id, sort_order, recommendation_status, recommendation_note,
                                  recommendation_reviewed_by, recommendation_reviewed_at) VALUES
    ((SELECT id FROM fx WHERE key = 'd_main'), (SELECT id FROM fx WHERE key = 'w_paint'), 0, 'VERIFIED',
     'This professional is recommended by the store based on their local referral network.', 1, now()),
    ((SELECT id FROM fx WHERE key = 'd_main'), (SELECT id FROM fx WHERE key = 'w_carp'), 1, 'PENDING',
     'Pending review - must not appear publicly yet.', NULL, NULL),
    ((SELECT id FROM fx WHERE key = 'd_main'), (SELECT id FROM fx WHERE key = 'w_elec'), 2, 'NONE', NULL, NULL, NULL),
    ((SELECT id FROM fx WHERE key = 'd_main'), (SELECT id FROM fx WHERE key = 'w_pending'), 3, 'NONE', NULL, NULL, NULL),
    ((SELECT id FROM fx WHERE key = 'd_sim2'), (SELECT id FROM fx WHERE key = 'w_paint'), 0, 'VERIFIED',
     'Has completed several painting jobs for our customers.', 1, now() - interval '1 day');

-- ---------- one approved review from a fixture user (keeps the aggregate consistent) ----------
INSERT INTO users (phone_number, role, email, name) VALUES ('+919910000099', 'CUSTOMER', 'reviewer@fixtures.sfs.local', 'Fixture Reviewer');
INSERT INTO business_review (business_id, user_id, reviewer_name, reviewer_location, rating, review_text,
                             moderation_status, moderated_by_dashboard_user_id, moderated_at)
VALUES ((SELECT id FROM fx WHERE key = 'd_main'), (SELECT id FROM users WHERE email = 'reviewer@fixtures.sfs.local'),
        'Neha Arora', 'Gurugram', 5,
        'Helpful staff who matched the exact shade we needed, and delivery the same evening.', 'APPROVED', 1, now());
UPDATE business SET avg_rating = 5.0, total_ratings = 1 WHERE id = (SELECT id FROM fx WHERE key = 'd_main');

SELECT key, id FROM fx ORDER BY key;

COMMIT;
