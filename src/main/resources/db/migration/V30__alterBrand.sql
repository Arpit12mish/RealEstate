ALTER TABLE brand
  ADD COLUMN IF NOT EXISTS promo_media_type varchar(20),
  ADD COLUMN IF NOT EXISTS promo_media_url text,
  ADD COLUMN IF NOT EXISTS promo_enabled boolean NOT NULL DEFAULT false;
