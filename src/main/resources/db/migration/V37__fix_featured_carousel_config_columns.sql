-- Ensure table exists (only if your DB already has it)
-- If table doesn't exist, remove this block and instead create table in a new migration.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'featured_carousel_config'
  ) THEN

    -- add missing columns safely
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='featured_carousel_config' AND column_name='enabled'
    ) THEN
      ALTER TABLE featured_carousel_config
        ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='featured_carousel_config' AND column_name='sort_order'
    ) THEN
      ALTER TABLE featured_carousel_config
        ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_name='featured_carousel_config' AND column_name='logo_url'
    ) THEN
      ALTER TABLE featured_carousel_config
        ADD COLUMN logo_url TEXT NULL;
    END IF;

  END IF;
END $$;
