-- V118's partial unique indexes on brand_collaboration (brand_id, <target>_id)
-- did not exclude soft-deleted rows, so a soft-deleted collaboration (deleted = true)
-- permanently blocked recreating the same brand+target pair - the admin could delete
-- a collaboration but never re-add it without a manual data fix.
--
-- This migration replaces those four indexes with partial unique indexes scoped to
-- deleted = false, matching the app-level duplicate check
-- (BrandCollaborationRepository.existsByBrand_IdAnd<Target>_IdAndDeletedFalse) that
-- already only considered non-deleted rows. V118 itself is untouched; brand_collaboration
-- has no rows in any environment this has run against, so there is no data to migrate.
--
-- Plain CREATE/DROP INDEX (not CONCURRENTLY) matches V118's own style and is required
-- here anyway since Flyway runs each migration inside a transaction.

DROP INDEX IF EXISTS uk_brand_collaboration_project;
DROP INDEX IF EXISTS uk_brand_collaboration_builder;
DROP INDEX IF EXISTS uk_brand_collaboration_company;
DROP INDEX IF EXISTS uk_brand_collaboration_business;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_project
  ON brand_collaboration (brand_id, project_id)
  WHERE project_id IS NOT NULL AND deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_builder
  ON brand_collaboration (brand_id, builder_id)
  WHERE builder_id IS NOT NULL AND deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_company
  ON brand_collaboration (brand_id, company_id)
  WHERE company_id IS NOT NULL AND deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_business
  ON brand_collaboration (brand_id, business_id)
  WHERE business_id IS NOT NULL AND deleted = false;
