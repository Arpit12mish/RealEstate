-- Brand detail page FAQ section.

CREATE TABLE IF NOT EXISTS brand_faq (
  id          BIGSERIAL PRIMARY KEY,
  brand_id    BIGINT NOT NULL,
  question    VARCHAR(300) NOT NULL,
  answer      TEXT NOT NULL,
  sort_order  INTEGER NOT NULL DEFAULT 0,
  active      BOOLEAN NOT NULL DEFAULT true,
  deleted     BOOLEAN NOT NULL DEFAULT false,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_brand_faq_brand'
  ) THEN
    ALTER TABLE brand_faq
      ADD CONSTRAINT fk_brand_faq_brand FOREIGN KEY (brand_id) REFERENCES brand(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_brand_faq_brand_sort ON brand_faq (brand_id, sort_order, id);
