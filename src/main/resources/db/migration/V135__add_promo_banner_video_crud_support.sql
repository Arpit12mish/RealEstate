ALTER TABLE promo_banner
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_promo_banner_media_type'
    ) THEN
        ALTER TABLE promo_banner
            ADD CONSTRAINT chk_promo_banner_media_type
            CHECK (media_type IN ('IMAGE', 'LOTTIE_JSON', 'VIDEO'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_promo_banner_public_slot
    ON promo_banner (category_id, slot_key, is_active, is_deleted, priority, id);
