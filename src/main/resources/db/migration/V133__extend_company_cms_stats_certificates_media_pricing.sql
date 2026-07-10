-- Phase 4.8B: Rich Company Profile CMS. Extends the existing company_stat and
-- company_certificate tables (both already used read-only by
-- ArchitectDesignerPublicServiceImpl) with the fields their new dashboard CRUD
-- needs, rather than creating duplicate tables. Adds two genuinely new tables
-- (company_media, company_pricing_plan) since nothing like them exists today.

ALTER TABLE company_stat
  ADD COLUMN IF NOT EXISTS icon_key VARCHAR(60),
  ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE company_certificate
  ADD COLUMN IF NOT EXISTS description TEXT,
  ADD COLUMN IF NOT EXISTS certificate_file_url TEXT,
  ADD COLUMN IF NOT EXISTS year INTEGER,
  ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT true;

-- Hero/gallery images for the company profile. logoUrl/coverImageUrl on company
-- stay as-is (single-image fields); this is the separate multi-image set the
-- mobile hero carousel needs, filtered by usage_type.
CREATE TABLE IF NOT EXISTS company_media (
  id                BIGSERIAL PRIMARY KEY,
  company_id        BIGINT NOT NULL REFERENCES company(id),
  media_url         TEXT NOT NULL,
  media_type        VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  usage_type        VARCHAR(20) NOT NULL,
  title             VARCHAR(180),
  alt_text          VARCHAR(255),
  sort_order        INTEGER NOT NULL DEFAULT 0,
  public_visible    BOOLEAN NOT NULL DEFAULT true,
  active            BOOLEAN NOT NULL DEFAULT true,
  deleted           BOOLEAN NOT NULL DEFAULT false,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_company_media_usage_type CHECK (usage_type IN ('HERO', 'GALLERY', 'CARD')),
  CONSTRAINT chk_company_media_media_type CHECK (media_type IN ('IMAGE'))
);

CREATE INDEX IF NOT EXISTS idx_company_media_company ON company_media (company_id);
CREATE INDEX IF NOT EXISTS idx_company_media_lookup
  ON company_media (company_id, usage_type, deleted, active, public_visible);

-- Pricing plan cards (Subscription / Project-Based tabs on mobile).
CREATE TABLE IF NOT EXISTS company_pricing_plan (
  id                BIGSERIAL PRIMARY KEY,
  company_id        BIGINT NOT NULL REFERENCES company(id),
  pricing_type      VARCHAR(20) NOT NULL,
  plan_name         VARCHAR(100) NOT NULL,
  price_amount      NUMERIC(12,2),
  currency          VARCHAR(10) NOT NULL DEFAULT 'INR',
  billing_unit      VARCHAR(30),
  description       TEXT,
  features_json     JSONB NOT NULL DEFAULT '[]',
  sort_order        INTEGER NOT NULL DEFAULT 0,
  public_visible    BOOLEAN NOT NULL DEFAULT true,
  active            BOOLEAN NOT NULL DEFAULT true,
  deleted           BOOLEAN NOT NULL DEFAULT false,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_company_pricing_plan_type CHECK (pricing_type IN ('SUBSCRIPTION', 'PROJECT_BASED')),
  CONSTRAINT chk_company_pricing_plan_price CHECK (price_amount IS NULL OR price_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_company_pricing_plan_company ON company_pricing_plan (company_id);
CREATE INDEX IF NOT EXISTS idx_company_pricing_plan_lookup
  ON company_pricing_plan (company_id, pricing_type, deleted, active, public_visible);
