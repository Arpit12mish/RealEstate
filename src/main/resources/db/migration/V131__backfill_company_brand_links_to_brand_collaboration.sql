-- One-time backfill from legacy company_brand_link into canonical
-- brand_collaboration for company profile connected-brand cards.
--
-- company_brand_link remains in place as a legacy fallback. This migration
-- only inserts public-safe active links that do not already have a non-deleted
-- canonical COMPANY collaboration, so existing brand_collaboration rows win.

INSERT INTO brand_collaboration (
  brand_id,
  target_type,
  company_id,
  project_id,
  builder_id,
  business_id,
  company_project_id,
  active,
  deleted,
  public_visible,
  verified,
  featured,
  source_type,
  relation_type,
  sort_order,
  priority,
  role,
  usage_category,
  title,
  description
)
SELECT
  cbl.brand_id,
  'COMPANY',
  cbl.company_id,
  NULL,
  NULL,
  NULL,
  NULL,
  TRUE,
  FALSE,
  TRUE,
  FALSE,
  FALSE,
  'ADMIN_ENTERED',
  NULL,
  COALESCE(cbl.sort_order, 0),
  0,
  NULL,
  NULL,
  NULL,
  NULL
FROM company_brand_link cbl
JOIN company c ON c.id = cbl.company_id
JOIN brand b ON b.id = cbl.brand_id
WHERE cbl.active = TRUE
  AND cbl.deleted = FALSE
  AND c.active = TRUE
  AND c.published = TRUE
  AND c.deleted = FALSE
  AND b.active = TRUE
  AND b.published = TRUE
  AND b.deleted = FALSE
  AND NOT EXISTS (
    SELECT 1
    FROM brand_collaboration bc
    WHERE bc.brand_id = cbl.brand_id
      AND bc.company_id = cbl.company_id
      AND bc.target_type = 'COMPANY'
      AND bc.deleted = FALSE
  );
