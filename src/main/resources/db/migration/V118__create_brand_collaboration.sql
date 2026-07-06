-- Brand <-> Project/Builder/Company/Business connections ("connected brands").
--
-- Uses discrete nullable foreign keys plus a target_type discriminator,
-- rather than a polymorphic target_id without FK enforcement. This matches
-- every existing cross-entity link table in this codebase (company_brand_link,
-- company_project, brand_distributor, project.builder_id, ...) and keeps
-- referential integrity enforced by Postgres, which matters here because the
-- schema has no ddl-auto safety net (Flyway is the only source of truth).
--
-- Architects and interior designers are both `company` rows (distinguished
-- by company.company_type) so both are reached via target_type = 'COMPANY'.

CREATE TABLE IF NOT EXISTS brand_collaboration (
  id           BIGSERIAL PRIMARY KEY,
  brand_id     BIGINT NOT NULL,
  target_type  VARCHAR(20) NOT NULL,
  project_id   BIGINT,
  builder_id   BIGINT,
  company_id   BIGINT,
  business_id  BIGINT,
  role         VARCHAR(50),
  sort_order   INTEGER NOT NULL DEFAULT 0,
  active       BOOLEAN NOT NULL DEFAULT true,
  priority     INTEGER NOT NULL DEFAULT 0,
  deleted      BOOLEAN NOT NULL DEFAULT false,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_collaboration_brand'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_collaboration_project'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_project FOREIGN KEY (project_id) REFERENCES project(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_collaboration_builder'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_builder FOREIGN KEY (builder_id) REFERENCES builder(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_collaboration_company'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_company FOREIGN KEY (company_id) REFERENCES company(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_collaboration_business'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT fk_brand_collaboration_business FOREIGN KEY (business_id) REFERENCES business(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_brand_collaboration_target_type'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT chk_brand_collaboration_target_type
      CHECK (target_type IN ('PROJECT', 'BUILDER', 'COMPANY', 'BUSINESS'));
  END IF;

  -- Exactly one of the four target FKs must be set, and it must match target_type.
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_brand_collaboration_single_target'
  ) THEN
    ALTER TABLE brand_collaboration
      ADD CONSTRAINT chk_brand_collaboration_single_target
      CHECK (
        (target_type = 'PROJECT'  AND project_id  IS NOT NULL AND builder_id IS NULL     AND company_id IS NULL     AND business_id IS NULL)
        OR (target_type = 'BUILDER'  AND builder_id  IS NOT NULL AND project_id IS NULL     AND company_id IS NULL     AND business_id IS NULL)
        OR (target_type = 'COMPANY'  AND company_id  IS NOT NULL AND project_id IS NULL     AND builder_id IS NULL     AND business_id IS NULL)
        OR (target_type = 'BUSINESS' AND business_id IS NOT NULL AND project_id IS NULL     AND builder_id IS NULL     AND company_id IS NULL)
      );
  END IF;
END $$;

-- One collaboration per (brand, target) pair, per target type.
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_project  ON brand_collaboration (brand_id, project_id)  WHERE project_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_builder  ON brand_collaboration (brand_id, builder_id)  WHERE builder_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_company  ON brand_collaboration (brand_id, company_id)  WHERE company_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_collaboration_business ON brand_collaboration (brand_id, business_id) WHERE business_id IS NOT NULL;

-- Brand -> its collaborations (dashboard list, brand detail stats).
CREATE INDEX IF NOT EXISTS idx_brand_collaboration_brand ON brand_collaboration (brand_id, active, deleted, sort_order);

-- Reverse lookups: target -> its connected brands (project/builder/company/business detail pages).
CREATE INDEX IF NOT EXISTS idx_brand_collaboration_by_project  ON brand_collaboration (project_id, active, deleted, sort_order)  WHERE project_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_brand_collaboration_by_builder  ON brand_collaboration (builder_id, active, deleted, sort_order)  WHERE builder_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_brand_collaboration_by_company  ON brand_collaboration (company_id, active, deleted, sort_order)  WHERE company_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_brand_collaboration_by_business ON brand_collaboration (business_id, active, deleted, sort_order) WHERE business_id IS NOT NULL;
