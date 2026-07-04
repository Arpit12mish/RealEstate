ALTER TABLE promo_banner
ADD COLUMN IF NOT EXISTS display_duration_ms INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_promo_banner_display_duration_ms'
    ) THEN
        ALTER TABLE promo_banner
        ADD CONSTRAINT chk_promo_banner_display_duration_ms
        CHECK (
            display_duration_ms IS NULL
            OR (display_duration_ms BETWEEN 1000 AND 60000)
        );
    END IF;
END $$;

UPDATE promo_banner
SET display_duration_ms = 12000
WHERE media_type = 'LOTTIE_JSON'
  AND media_url = 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/hero-animation.json';
