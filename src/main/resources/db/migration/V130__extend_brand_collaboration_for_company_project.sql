-- Extends brand_collaboration with COMPANY_PROJECT support for architect/designer
-- portfolio project "Brands Used" cards. This keeps brand_collaboration canonical
-- instead of introducing another company_project_brand_link table.

ALTER TABLE brand_collaboration
  ADD COLUMN IF NOT EXISTS company_project_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_brand_collaboration_company_project'
      AND conrelid = 'brand_collaboration'::regclass
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_company_project
      FOREIGN KEY (company_project_id) REFERENCES company_project(id);
  END IF;
END $$;

ALTER TABLE brand_collaboration
  DROP CONSTRAINT IF EXISTS chk_brand_collaboration_target_type;

ALTER TABLE brand_collaboration
  ADD CONSTRAINT chk_brand_collaboration_target_type
  CHECK (target_type IN ('PROJECT', 'BUILDER', 'COMPANY', 'BUSINESS', 'COMPANY_PROJECT'));

ALTER TABLE brand_collaboration
  DROP CONSTRAINT IF EXISTS chk_brand_collaboration_single_target;

ALTER TABLE brand_collaboration
  ADD CONSTRAINT chk_brand_collaboration_single_target
  CHECK (
    (target_type = 'PROJECT'
      AND project_id IS NOT NULL
      AND builder_id IS NULL
      AND company_id IS NULL
      AND business_id IS NULL
      AND company_project_id IS NULL)
    OR (target_type = 'BUILDER'
      AND builder_id IS NOT NULL
      AND project_id IS NULL
      AND company_id IS NULL
      AND business_id IS NULL
      AND company_project_id IS NULL)
    OR (target_type = 'COMPANY'
      AND company_id IS NOT NULL
      AND project_id IS NULL
      AND builder_id IS NULL
      AND business_id IS NULL
      AND company_project_id IS NULL)
    OR (target_type = 'BUSINESS'
      AND business_id IS NOT NULL
      AND project_id IS NULL
      AND builder_id IS NULL
      AND company_id IS NULL
      AND company_project_id IS NULL)
    OR (target_type = 'COMPANY_PROJECT'
      AND company_project_id IS NOT NULL
      AND project_id IS NULL
      AND builder_id IS NULL
      AND company_id IS NULL
      AND business_id IS NULL)
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_company_project
  ON brand_collaboration (brand_id, company_project_id)
  WHERE company_project_id IS NOT NULL AND deleted = false;

CREATE INDEX IF NOT EXISTS idx_brand_collaboration_by_company_project
  ON brand_collaboration (company_project_id, active, deleted, sort_order)
  WHERE company_project_id IS NOT NULL;
