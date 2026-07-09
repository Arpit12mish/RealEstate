-- Recreate/harden promo_banner_slot_config so Home promo placement is reproducible
-- in fresh environments and consistent with PromoBannerSlotConfigEntity.

CREATE TABLE IF NOT EXISTS promo_banner_slot_config (
    id BIGSERIAL PRIMARY KEY,
    screen VARCHAR(20) NOT NULL,
    home_category_id BIGINT NOT NULL,
    city_id BIGINT NULL,
    slot_key VARCHAR(20) NOT NULL DEFAULT 'HERO',
    insert_after_section_type VARCHAR(50) NULL,
    position_index INTEGER NOT NULL DEFAULT 0,
    max_items INTEGER NOT NULL DEFAULT 10,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_at TIMESTAMPTZ NULL,
    end_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Existing DB hardening. Safe no-op for fresh DBs.

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS screen VARCHAR(20);

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS home_category_id BIGINT;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS city_id BIGINT;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS slot_key VARCHAR(20) DEFAULT 'HERO';

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS insert_after_section_type VARCHAR(50);

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS position_index INTEGER DEFAULT 0;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS max_items INTEGER DEFAULT 10;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS start_at TIMESTAMPTZ;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS end_at TIMESTAMPTZ;

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE promo_banner_slot_config
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Fill nulls before enforcing NOT NULL.

UPDATE promo_banner_slot_config
SET slot_key = 'HERO'
WHERE slot_key IS NULL;

UPDATE promo_banner_slot_config
SET position_index = 0
WHERE position_index IS NULL;

UPDATE promo_banner_slot_config
SET max_items = 10
WHERE max_items IS NULL;

UPDATE promo_banner_slot_config
SET priority = 0
WHERE priority IS NULL;

UPDATE promo_banner_slot_config
SET is_active = TRUE
WHERE is_active IS NULL;

UPDATE promo_banner_slot_config
SET created_at = NOW()
WHERE created_at IS NULL;

UPDATE promo_banner_slot_config
SET updated_at = NOW()
WHERE updated_at IS NULL;

-- Match PromoBannerSlotConfigEntity nullable=false fields.

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN screen SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN home_category_id SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN slot_key SET DEFAULT 'HERO';

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN slot_key SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN position_index SET DEFAULT 0;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN position_index SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN max_items SET DEFAULT 10;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN max_items SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN priority SET DEFAULT 0;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN priority SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN is_active SET DEFAULT TRUE;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE promo_banner_slot_config
    ALTER COLUMN updated_at SET NOT NULL;

-- Indexes matching the entity plus query patterns.

CREATE INDEX IF NOT EXISTS idx_promo_slot_category_id
    ON promo_banner_slot_config (home_category_id);

CREATE INDEX IF NOT EXISTS idx_promo_slot_city_id
    ON promo_banner_slot_config (city_id);

CREATE INDEX IF NOT EXISTS idx_promo_slot_screen
    ON promo_banner_slot_config (screen);

CREATE INDEX IF NOT EXISTS idx_promo_slot_screen_category_active_priority
    ON promo_banner_slot_config (screen, home_category_id, is_active, priority, id);

CREATE INDEX IF NOT EXISTS idx_promo_slot_screen_category_city_active_priority
    ON promo_banner_slot_config (screen, home_category_id, city_id, is_active, priority, id);