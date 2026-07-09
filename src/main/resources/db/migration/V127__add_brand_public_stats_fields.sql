ALTER TABLE brand
  ADD COLUMN IF NOT EXISTS founded_year INTEGER,
  ADD COLUMN IF NOT EXISTS customer_rating NUMERIC(2,1),
  ADD COLUMN IF NOT EXISTS customer_rating_count INTEGER;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_brand_founded_year_range'
  ) THEN
    ALTER TABLE brand
      ADD CONSTRAINT chk_brand_founded_year_range
      CHECK (founded_year IS NULL OR founded_year BETWEEN 1800 AND 2200);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_brand_customer_rating_range'
  ) THEN
    ALTER TABLE brand
      ADD CONSTRAINT chk_brand_customer_rating_range
      CHECK (customer_rating IS NULL OR customer_rating BETWEEN 0 AND 5);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_brand_customer_rating_count_non_negative'
  ) THEN
    ALTER TABLE brand
      ADD CONSTRAINT chk_brand_customer_rating_count_non_negative
      CHECK (customer_rating_count IS NULL OR customer_rating_count >= 0);
  END IF;
END $$;
