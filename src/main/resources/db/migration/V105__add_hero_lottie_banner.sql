-- Add slot_key to promo_banner if it doesn't already exist (may have been added via DDL auto)
ALTER TABLE promo_banner ADD COLUMN IF NOT EXISTS slot_key VARCHAR(20) NOT NULL DEFAULT 'HERO';

-- Add media_type and media_url for mixed-media carousel support
ALTER TABLE promo_banner ADD COLUMN IF NOT EXISTS media_type VARCHAR(30) NOT NULL DEFAULT 'IMAGE';
ALTER TABLE promo_banner ADD COLUMN IF NOT EXISTS media_url TEXT;

-- Lottie banners have no imageUrl; make the column nullable
ALTER TABLE promo_banner ALTER COLUMN image_url DROP NOT NULL;

-- Seed the HERO Lottie animation slide (idempotent)
INSERT INTO promo_banner (
    category_id,
    title,
    subtitle,
    image_url,
    media_type,
    media_url,
    target_url,
    priority,
    is_active,
    slot_key,
    created_at,
    updated_at
)
SELECT
    0,
    'Compare Smarter',
    'Compare projects side by side',
    NULL,
    'LOTTIE_JSON',
    'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/hero-animation.json',
    '/compare-projects/select',
    2,
    TRUE,
    'HERO',
    NOW(),
    NOW()
WHERE EXISTS (SELECT 1 FROM category WHERE id = 0)
  AND NOT EXISTS (
    SELECT 1 FROM promo_banner
    WHERE media_type = 'LOTTIE_JSON'
      AND media_url   = 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/hero-animation.json'
);
