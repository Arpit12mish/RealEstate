-- =========================================
-- HOME SECTION ITEMS (curated feed items)
-- =========================================

CREATE TABLE IF NOT EXISTS home_section_item (
  id BIGSERIAL PRIMARY KEY,

  home_category_id BIGINT NOT NULL REFERENCES category(id),
  section_type VARCHAR(50) NOT NULL,      -- e.g. COMPANIES
  item_type VARCHAR(30) NOT NULL,         -- BRAND / BUILDER / BUSINESS
  ref_id BIGINT NOT NULL,                 -- points to brand.id / builder.id / business.id

  title VARCHAR(150),                     -- optional override title per card
  subtitle VARCHAR(255),                  -- optional override
  image_url TEXT,                         -- optional override (rarely needed)

  sort_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_home_section_item_lookup
  ON home_section_item (home_category_id, section_type, active, deleted, sort_order, id);

CREATE INDEX IF NOT EXISTS ix_home_section_item_ref
  ON home_section_item (item_type, ref_id);
