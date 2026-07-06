-- Renames the home_section_config row seeded by V122 from TOP_BRANDS to CONNECTED_BRANDS
-- (Phase 1D.6): product wants the home brand showcase to represent brands
-- connected/collaborated with projects and partners, not a generic "top brands" list.
-- V122 itself is untouched - this is a follow-up UPDATE, not a revision of it.
--
-- Idempotent and safe to re-run:
--   - Only touches the row if it is still TOP_BRANDS for the global feed
--     (home_category_id = 0) - a no-op on any environment where this already ran.
--   - Guarded by NOT EXISTS so it can never create a duplicate CONNECTED_BRANDS row if
--     one was somehow already seeded independently; in that case this UPDATE simply
--     does nothing; that already exists.

UPDATE home_section_config
SET
  section_type = 'CONNECTED_BRANDS',
  title = 'Connected Brands',
  subtitle = 'Trusted brands connected with projects and partners',
  updated_at = NOW()
WHERE home_category_id = 0
  AND section_type = 'TOP_BRANDS'
  AND NOT EXISTS (
    SELECT 1
    FROM home_section_config
    WHERE home_category_id = 0
      AND section_type = 'CONNECTED_BRANDS'
  );
