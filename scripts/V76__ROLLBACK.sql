-- =============================================================================
-- ROLLBACK for V76__seed_knowledge_center_architect_designer.sql
-- =============================================================================
-- This file is NOT run by Flyway. Execute it manually on psql / RDS console
-- ONLY if you need to undo V76 after it has been applied.
--
-- After running this rollback you must also delete the V76 checksum from:
--   DELETE FROM flyway_schema_history WHERE version = '76';
-- Otherwise Flyway will refuse to re-apply V76 later.
-- =============================================================================

BEGIN;

-- 1. Remove brand links created for Knowledge Center
DELETE FROM company_brand_link
WHERE company_id = (SELECT id FROM company WHERE slug = 'knowledge-center');

-- 2. Remove service/project cards
DELETE FROM company_project
WHERE slug IN (
    'knowledge-center-immersive-hub',
    'knowledge-center-business-center'
);

-- 3. Remove stats
DELETE FROM company_stat
WHERE company_id = (SELECT id FROM company WHERE slug = 'knowledge-center');

-- 4. Remove the company
DELETE FROM company
WHERE slug = 'knowledge-center';

-- 5. Remove the 4 brands inserted by V76 — only if they have no other company links
--    (Berger Paints is excluded; it pre-existed and was not inserted by V76)
DELETE FROM brand
WHERE LOWER(name) IN ('alex', 'duro', 'dee pearls', 'incor')
  AND NOT EXISTS (
      SELECT 1
      FROM company_brand_link cbl
      WHERE cbl.brand_id = brand.id
  );

-- 6. Remove architect subcategories inserted by V76
--    Safe: only deletes by known slugs; does NOT touch the parent 'architects' row
DELETE FROM category
WHERE slug IN (
    'residential-architects',
    'commercial-architects',
    'interior-architects',
    'landscape-architects',
    '3d-elevation-designers',
    'vastu-architects',
    'structural-consultants'
);

-- 7. Remove Flyway history entry so V76 can be cleanly re-applied if needed
DELETE FROM flyway_schema_history WHERE version = '76';

COMMIT;
