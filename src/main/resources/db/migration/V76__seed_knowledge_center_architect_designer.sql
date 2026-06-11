-- =============================================================================
-- V76__seed_knowledge_center_architect_designer.sql
-- =============================================================================
-- Purpose:
--   Seed Knowledge Center as an ARCHITECT&DESIGNERS company.
--   Seed architect subcategories, brands, stats, service/project cards,
--   and company-brand links.
--
-- Important:
--   Data-only migration.
--   Do not put schema/permission/table-structure changes in this file.
--
-- Required existing tables:
--   category, brand, company, company_stat, company_project, company_brand_link
-- =============================================================================

-- =============================================================================
-- SECTION 1: Ensure parent Architects category exists
-- =============================================================================

INSERT INTO category (name, slug, parent_id, priority, active, created_at, updated_at)
VALUES ('Architects', 'architects', NULL, 10, TRUE, NOW(), NOW())
ON CONFLICT (slug)
DO UPDATE SET
    name = EXCLUDED.name,
    priority = EXCLUDED.priority,
    active = TRUE,
    updated_at = NOW();


-- =============================================================================
-- SECTION 2: Architect subcategories
-- =============================================================================

WITH parent AS (
    SELECT id
    FROM category
    WHERE slug = 'architects'
)
INSERT INTO category (name, slug, parent_id, priority, active, created_at, updated_at)
SELECT v.name, v.slug, parent.id, v.priority, TRUE, NOW(), NOW()
FROM parent,
     (VALUES
          ('Residential Architects', 'residential-architects', 1),
          ('Commercial Architects',  'commercial-architects',  2),
          ('Interior Architects',    'interior-architects',    3),
          ('Landscape Architects',   'landscape-architects',   4),
          ('3D Elevation Designers', '3d-elevation-designers', 5),
          ('Vastu Architects',       'vastu-architects',       6),
          ('Structural Consultants', 'structural-consultants', 7)
     ) AS v(name, slug, priority)
ON CONFLICT (slug)
DO UPDATE SET
    name = EXCLUDED.name,
    parent_id = EXCLUDED.parent_id,
    priority = EXCLUDED.priority,
    active = TRUE,
    updated_at = NOW();


-- =============================================================================
-- SECTION 3: Insert 4 new brands
-- brand table has no unique constraint on name, so use WHERE NOT EXISTS.
-- Berger Paints is skipped because it already exists in production.
-- =============================================================================

INSERT INTO brand (name, logo_url, active, published, priority, deleted, created_at, updated_at)
SELECT 'Alex',
       'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/alex.png',
       TRUE, TRUE, 0, FALSE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM brand WHERE LOWER(name) = 'alex' AND deleted = FALSE
);

INSERT INTO brand (name, logo_url, active, published, priority, deleted, created_at, updated_at)
SELECT 'DURO',
       'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/DURO.png',
       TRUE, TRUE, 0, FALSE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM brand WHERE LOWER(name) = 'duro' AND deleted = FALSE
);

INSERT INTO brand (name, logo_url, active, published, priority, deleted, created_at, updated_at)
SELECT 'Dee Pearls',
       'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/deepearls.png',
       TRUE, TRUE, 0, FALSE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM brand WHERE LOWER(name) = 'dee pearls' AND deleted = FALSE
);

INSERT INTO brand (name, logo_url, active, published, priority, deleted, created_at, updated_at)
SELECT 'Incor',
       'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/incor.png',
       TRUE, TRUE, 0, FALSE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM brand WHERE LOWER(name) = 'incor' AND deleted = FALSE
);


-- =============================================================================
-- SECTION 4: Insert or update Knowledge Center company
-- company_type must remain ARCHITECT&DESIGNERS because the section loaders match it.
-- =============================================================================

INSERT INTO company (
    name,
    slug,
    company_type,
    logo_url,
    cover_image_url,
    description,
    specialization_text,
    info_line_1,
    info_line_2,
    phone,
    whatsapp,
    email,
    active,
    published,
    priority,
    deleted,
    created_at,
    updated_at
)
VALUES (
    'Knowledge Center',
    'knowledge-center',
    'ARCHITECT&DESIGNERS',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/thumbnail.webp',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/immersive-hub-main.webp',
    'The KC Knowledge Center (KC) is India''s largest specialized physical and digital ecosystem curated entirely for the architecture, building materials, and design community. Founded during the COVID-19 pandemic in 2020 and brought to life through physical deployment, it is structured as a permanent, tech-enabled, curated workspace and sourcing exhibition that brings architects, designers, builders, manufacturers, and homeowners together under one roof.',
    'Architecture | Interior Design | Material Library | Events',
    'India''s largest architecture & design ecosystem',
    'M3M Broadway, Gurgaon',
    '+919811341410',
    '+919811341410',
    'info@squarefootstory.com',
    TRUE,
    TRUE,
    1,
    FALSE,
    NOW(),
    NOW()
)
ON CONFLICT (slug)
DO UPDATE SET
    name = EXCLUDED.name,
    company_type = EXCLUDED.company_type,
    logo_url = EXCLUDED.logo_url,
    cover_image_url = EXCLUDED.cover_image_url,
    description = EXCLUDED.description,
    specialization_text = EXCLUDED.specialization_text,
    info_line_1 = EXCLUDED.info_line_1,
    info_line_2 = EXCLUDED.info_line_2,
    phone = EXCLUDED.phone,
    whatsapp = EXCLUDED.whatsapp,
    email = EXCLUDED.email,
    active = EXCLUDED.active,
    published = EXCLUDED.published,
    priority = EXCLUDED.priority,
    deleted = FALSE,
    updated_at = NOW();


-- =============================================================================
-- SECTION 5: Insert company stats
-- Labels must match section loader expectations.
-- =============================================================================

INSERT INTO company_stat (company_id, label, value, display_order, active, deleted, created_at, updated_at)
SELECT c.id, 'PROJECTS COMPLETED', '200+', 1, TRUE, FALSE, NOW(), NOW()
FROM company c
WHERE c.slug = 'knowledge-center'
  AND NOT EXISTS (
      SELECT 1 FROM company_stat cs
      WHERE cs.company_id = c.id
        AND UPPER(cs.label) = 'PROJECTS COMPLETED'
        AND cs.deleted = FALSE
  );

INSERT INTO company_stat (company_id, label, value, display_order, active, deleted, created_at, updated_at)
SELECT c.id, 'YEARS EXPERIENCE', '6', 2, TRUE, FALSE, NOW(), NOW()
FROM company c
WHERE c.slug = 'knowledge-center'
  AND NOT EXISTS (
      SELECT 1 FROM company_stat cs
      WHERE cs.company_id = c.id
        AND UPPER(cs.label) = 'YEARS EXPERIENCE'
        AND cs.deleted = FALSE
  );

INSERT INTO company_stat (company_id, label, value, display_order, active, deleted, created_at, updated_at)
SELECT c.id, 'CITIES SERVED', '200+', 3, TRUE, FALSE, NOW(), NOW()
FROM company c
WHERE c.slug = 'knowledge-center'
  AND NOT EXISTS (
      SELECT 1 FROM company_stat cs
      WHERE cs.company_id = c.id
        AND UPPER(cs.label) = 'CITIES SERVED'
        AND cs.deleted = FALSE
  );


-- =============================================================================
-- SECTION 6: Insert service/project cards
-- =============================================================================

INSERT INTO company_project (
    company_id, name, slug, description, address_line,
    tags, cover_media_url, cover_media_type,
    active, published, priority, deleted, created_at, updated_at
)
SELECT
    c.id,
    'Immersive Hub',
    'knowledge-center-immersive-hub',
    'Material Library — Curated material samples with real-scale display, side-by-side comparison, and detailed technical specifications.',
    'Knowledge Center, M3M Broadway, Gurgaon, Sector 71',
    '2000 brand ecosystem,Real scale samples,Detailed technical specifications,Side by side comparison',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/immersive-hub-main.webp',
    'IMAGE',
    TRUE, TRUE, 1, FALSE, NOW(), NOW()
FROM company c
WHERE c.slug = 'knowledge-center'
ON CONFLICT (slug)
DO UPDATE SET
    company_id = EXCLUDED.company_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    address_line = EXCLUDED.address_line,
    tags = EXCLUDED.tags,
    cover_media_url = EXCLUDED.cover_media_url,
    cover_media_type = EXCLUDED.cover_media_type,
    active = EXCLUDED.active,
    published = EXCLUDED.published,
    priority = EXCLUDED.priority,
    deleted = FALSE,
    updated_at = NOW();

INSERT INTO company_project (
    company_id, name, slug, description, address_line,
    tags, cover_media_url, cover_media_type,
    active, published, priority, deleted, created_at, updated_at
)
SELECT
    c.id,
    'Business Center',
    'knowledge-center-business-center',
    'Spaces — Spacious professional workspace with elegant aesthetics, privacy focus, and high-speed internet connectivity.',
    'Knowledge Center, M3M Broadway, Gurgaon, Sector 71',
    'Spacious layout,Elegant aesthetics,Privacy focused,High speed internet',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/brandsLogo/Confrence+room+01.webp',
    'IMAGE',
    TRUE, TRUE, 2, FALSE, NOW(), NOW()
FROM company c
WHERE c.slug = 'knowledge-center'
ON CONFLICT (slug)
DO UPDATE SET
    company_id = EXCLUDED.company_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    address_line = EXCLUDED.address_line,
    tags = EXCLUDED.tags,
    cover_media_url = EXCLUDED.cover_media_url,
    cover_media_type = EXCLUDED.cover_media_type,
    active = EXCLUDED.active,
    published = EXCLUDED.published,
    priority = EXCLUDED.priority,
    deleted = FALSE,
    updated_at = NOW();


-- =============================================================================
-- SECTION 7: Link brands to Knowledge Center
-- company_brand_link should already have UNIQUE (company_id, brand_id).
-- =============================================================================

INSERT INTO company_brand_link (company_id, brand_id, sort_order, active, deleted, created_at, updated_at)
SELECT c.id, b.id, 1, TRUE, FALSE, NOW(), NOW()
FROM company c, brand b
WHERE c.slug = 'knowledge-center'
  AND LOWER(b.name) = 'alex'
  AND b.deleted = FALSE
ON CONFLICT (company_id, brand_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    active = TRUE,
    deleted = FALSE,
    updated_at = NOW();

INSERT INTO company_brand_link (company_id, brand_id, sort_order, active, deleted, created_at, updated_at)
SELECT c.id, b.id, 2, TRUE, FALSE, NOW(), NOW()
FROM company c, brand b
WHERE c.slug = 'knowledge-center'
  AND LOWER(b.name) = 'duro'
  AND b.deleted = FALSE
ON CONFLICT (company_id, brand_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    active = TRUE,
    deleted = FALSE,
    updated_at = NOW();

INSERT INTO company_brand_link (company_id, brand_id, sort_order, active, deleted, created_at, updated_at)
SELECT c.id, b.id, 3, TRUE, FALSE, NOW(), NOW()
FROM company c, brand b
WHERE c.slug = 'knowledge-center'
  AND LOWER(b.name) = 'dee pearls'
  AND b.deleted = FALSE
ON CONFLICT (company_id, brand_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    active = TRUE,
    deleted = FALSE,
    updated_at = NOW();

INSERT INTO company_brand_link (company_id, brand_id, sort_order, active, deleted, created_at, updated_at)
SELECT c.id, b.id, 4, TRUE, FALSE, NOW(), NOW()
FROM company c, brand b
WHERE c.slug = 'knowledge-center'
  AND LOWER(b.name) = 'incor'
  AND b.deleted = FALSE
ON CONFLICT (company_id, brand_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    active = TRUE,
    deleted = FALSE,
    updated_at = NOW();

INSERT INTO company_brand_link (company_id, brand_id, sort_order, active, deleted, created_at, updated_at)
SELECT c.id, b.id, 5, TRUE, FALSE, NOW(), NOW()
FROM company c, brand b
WHERE c.slug = 'knowledge-center'
  AND LOWER(b.name) = 'berger paints'
  AND b.deleted = FALSE
ON CONFLICT (company_id, brand_id)
DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    active = TRUE,
    deleted = FALSE,
    updated_at = NOW();
