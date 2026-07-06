-- Reconciles brand.category_id, its FK, and its index into Flyway history.
--
-- These objects already exist in at least one environment (added outside the
-- migration pipeline, with no tracked migration ever creating them) but are
-- required by the current BrandEntity.category mapping. This migration is
-- written to converge every environment to the same tracked state: a no-op
-- where the objects already exist, and a real create where they don't.
--
-- Nothing is renamed or dropped. BrandEntity.category is currently unused by
-- application code (no DTO/service path reads or writes it), so this is a
-- pure schema-safety migration, not a data change.

ALTER TABLE brand
  ADD COLUMN IF NOT EXISTS category_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_category'
  ) THEN
    ALTER TABLE brand
      ADD CONSTRAINT fk_brand_category FOREIGN KEY (category_id) REFERENCES category(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_category ON brand (category_id);
