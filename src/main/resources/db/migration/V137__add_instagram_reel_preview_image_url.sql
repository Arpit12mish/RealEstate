ALTER TABLE instagram_reel
    ADD COLUMN preview_image_url VARCHAR(1000);

COMMENT ON COLUMN instagram_reel.preview_image_url IS
    'Optional dashboard-admin custom image that overrides the Instagram-synced thumbnail_url when set. Never written by Meta sync.';
