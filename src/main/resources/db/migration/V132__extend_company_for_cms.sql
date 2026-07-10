-- Adds the fields the dashboard Company Profile CMS (Phase 4.8) needs that company
-- has never had: a city association (for filtering/display, matching company_project's
-- own city_id), a free-text address line, and a simple services-offered list. Purely
-- additive - existing company rows get NULL city_id/address_line/services_offered.

ALTER TABLE company
  ADD COLUMN IF NOT EXISTS city_id BIGINT,
  ADD COLUMN IF NOT EXISTS address_line TEXT,
  ADD COLUMN IF NOT EXISTS services_offered TEXT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'fk_company_city' AND conrelid = 'company'::regclass
  ) THEN
    ALTER TABLE company
      ADD CONSTRAINT fk_company_city FOREIGN KEY (city_id) REFERENCES city(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_company_city ON company (city_id);
