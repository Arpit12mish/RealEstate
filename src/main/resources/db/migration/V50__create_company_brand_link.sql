CREATE TABLE IF NOT EXISTS company_brand_link (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  brand_id BIGINT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ,
  CONSTRAINT fk_company_brand_link_company
    FOREIGN KEY (company_id) REFERENCES company(id),
  CONSTRAINT fk_company_brand_link_brand
    FOREIGN KEY (brand_id) REFERENCES brand(id),
  CONSTRAINT uk_company_brand_link_company_brand
    UNIQUE (company_id, brand_id)
);

CREATE INDEX IF NOT EXISTS idx_company_brand_link_company_sort
  ON company_brand_link(company_id, sort_order, id);