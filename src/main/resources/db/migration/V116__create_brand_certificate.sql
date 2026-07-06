-- Brand certificates & credentials section. Mirrors company_certificate's shape.

CREATE TABLE IF NOT EXISTS brand_certificate (
  id               BIGSERIAL PRIMARY KEY,
  brand_id         BIGINT NOT NULL,
  title            VARCHAR(180) NOT NULL,
  issuer           VARCHAR(180),
  certificate_url  TEXT,
  sort_order       INTEGER NOT NULL DEFAULT 0,
  active           BOOLEAN NOT NULL DEFAULT true,
  deleted          BOOLEAN NOT NULL DEFAULT false,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_certificate_brand'
  ) THEN
    ALTER TABLE brand_certificate
      ADD CONSTRAINT fk_brand_certificate_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_certificate_brand_sort ON brand_certificate (brand_id, sort_order, id);
