-- Marketplace dealer details + Allied Services worker details.
--
-- Dealers are existing `business` rows that are NOT a worker's own linked listing (that
-- distinction is derived from provider_profile.provider_type, never stored twice). Workers are
-- existing `provider_profile` rows with provider_type = 'WORKER'. This migration only adds the
-- fields/relationships the detail screens need; it never inserts listing data.

-- ---------------------------------------------------------------------------------------------
-- Dealer profile fields
-- ---------------------------------------------------------------------------------------------
ALTER TABLE business
    ADD COLUMN description TEXT,
    ADD COLUMN locality    VARCHAR(120),
    -- IANA zone used for opening-hours evaluation and years-in-business. Existing rows are all
    -- Indian listings, so the default is the correct backfill.
    ADD COLUMN timezone    VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata';

-- Weekly schedule. One row per opening interval; a day with no rows is closed. ISO day numbers
-- (1 = Monday). closes_at < opens_at is an overnight interval ending the next day;
-- closes_at = opens_at is a full 24-hour interval starting at opens_at.
CREATE TABLE business_opening_hours (
    id          BIGSERIAL PRIMARY KEY,
    business_id BIGINT      NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    day_of_week SMALLINT    NOT NULL,
    opens_at    TIME        NOT NULL,
    closes_at   TIME        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_opening_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT uk_business_opening_hours_start UNIQUE (business_id, day_of_week, opens_at)
);

CREATE INDEX idx_business_opening_hours_business ON business_opening_hours (business_id, day_of_week, opens_at);

-- Products ("Paints" -> Interior Paint, Exterior Paint ...) and services, as ordered groups of
-- ordered items.
CREATE TABLE business_offering_group (
    id            BIGSERIAL PRIMARY KEY,
    business_id   BIGINT       NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    offering_type VARCHAR(20)  NOT NULL,
    title         VARCHAR(120) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_offering_group_type CHECK (offering_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT chk_business_offering_group_title CHECK (length(btrim(title)) > 0)
);

CREATE UNIQUE INDEX uk_business_offering_group_title
    ON business_offering_group (business_id, offering_type, lower(title));
CREATE INDEX idx_business_offering_group_order
    ON business_offering_group (business_id, offering_type, sort_order, id);

CREATE TABLE business_offering_item (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT       NOT NULL REFERENCES business_offering_group (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_offering_item_name CHECK (length(btrim(name)) > 0)
);

CREATE UNIQUE INDEX uk_business_offering_item_name ON business_offering_item (group_id, lower(name));
CREATE INDEX idx_business_offering_item_order ON business_offering_item (group_id, sort_order, id);

-- Ordered dealer media. HERO feeds the top gallery, GALLERY feeds "Store Photos".
CREATE TABLE business_media (
    id          BIGSERIAL PRIMARY KEY,
    business_id BIGINT       NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    usage_type  VARCHAR(20)  NOT NULL,
    media_url   TEXT         NOT NULL,
    storage_key VARCHAR(500),
    alt_text    VARCHAR(255),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_media_usage CHECK (usage_type IN ('HERO', 'GALLERY')),
    CONSTRAINT chk_business_media_url CHECK (media_url ~* '^https://')
);

CREATE INDEX idx_business_media_public
    ON business_media (business_id, usage_type, sort_order, id)
    WHERE deleted = FALSE AND active = TRUE;

-- ---------------------------------------------------------------------------------------------
-- Worker profile fields
-- ---------------------------------------------------------------------------------------------
-- Worker availability is self/ops reported and unrelated to any store's opening hours.
ALTER TABLE provider_profile
    ADD COLUMN availability_status     VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN availability_updated_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_provider_profile_availability
        CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'UNAVAILABLE', 'UNKNOWN'));

CREATE TABLE provider_service_offering (
    id          BIGSERIAL PRIMARY KEY,
    provider_id BIGINT       NOT NULL REFERENCES provider_profile (id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_provider_service_offering_name CHECK (length(btrim(name)) > 0)
);

CREATE UNIQUE INDEX uk_provider_service_offering_name ON provider_service_offering (provider_id, lower(name));
CREATE INDEX idx_provider_service_offering_order ON provider_service_offering (provider_id, sort_order, id);

-- Typed worker charges. A visiting charge, a service fee and a material cost are different
-- things and are never relabelled from one generic amount. Exact decimal + ISO currency.
CREATE TABLE provider_rate (
    id          BIGSERIAL PRIMARY KEY,
    provider_id BIGINT        NOT NULL REFERENCES provider_profile (id) ON DELETE CASCADE,
    rate_type   VARCHAR(30)   NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'INR',
    unit        VARCHAR(20)   NOT NULL,
    note        VARCHAR(200),
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_provider_rate_type
        CHECK (rate_type IN ('VISITING_CHARGE', 'SERVICE_FEE', 'MATERIAL_COST', 'HOURLY_RATE', 'DAILY_RATE')),
    CONSTRAINT chk_provider_rate_unit
        CHECK (unit IN ('PER_VISIT', 'PER_HOUR', 'PER_DAY', 'PER_SQFT', 'PER_JOB')),
    CONSTRAINT chk_provider_rate_amount CHECK (amount >= 0),
    CONSTRAINT chk_provider_rate_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT uk_provider_rate_type UNIQUE (provider_id, rate_type)
);

CREATE INDEX idx_provider_rate_order ON provider_rate (provider_id, sort_order, id);

-- ---------------------------------------------------------------------------------------------
-- Dealer <-> worker relationship
-- ---------------------------------------------------------------------------------------------
-- A link means "this store works with / refers this worker" (Connected Workers). A link may
-- additionally carry a recommendation; only a dashboard ADMIN/REVIEWER can mark it VERIFIED,
-- and only VERIFIED recommendations appear in the worker's public "Recommended by".
CREATE TABLE business_worker_link (
    id                               BIGSERIAL PRIMARY KEY,
    business_id                      BIGINT       NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    provider_id                      BIGINT       NOT NULL REFERENCES provider_profile (id) ON DELETE CASCADE,
    status                           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    recommendation_status            VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    recommendation_note              VARCHAR(300),
    recommendation_reviewed_by       BIGINT,
    recommendation_reviewed_at       TIMESTAMPTZ,
    created_by_dashboard_user_id     BIGINT,
    sort_order                       INTEGER      NOT NULL DEFAULT 0,
    created_at                       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_worker_link_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_business_worker_link_recommendation
        CHECK (recommendation_status IN ('NONE', 'PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_business_worker_link_verified_reviewed
        CHECK (recommendation_status <> 'VERIFIED'
               OR (recommendation_reviewed_by IS NOT NULL AND recommendation_reviewed_at IS NOT NULL)),
    CONSTRAINT uk_business_worker_link UNIQUE (business_id, provider_id)
);

CREATE INDEX idx_business_worker_link_business ON business_worker_link (business_id, status, sort_order, id);
CREATE INDEX idx_business_worker_link_provider ON business_worker_link (provider_id, status, recommendation_status);

-- Relationship targets must be a WORKER provider and a dealer business (not a worker's own
-- linked listing). Cross-table, so enforced with a trigger rather than a CHECK.
CREATE FUNCTION enforce_business_worker_link_targets() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM provider_profile p
        WHERE p.id = NEW.provider_id AND p.provider_type = 'WORKER'
    ) THEN
        RAISE EXCEPTION 'business_worker_link.provider_id % is not a WORKER provider', NEW.provider_id
            USING ERRCODE = 'check_violation';
    END IF;

    IF EXISTS (
        SELECT 1 FROM provider_profile p
        WHERE p.business_id = NEW.business_id AND p.provider_type = 'WORKER'
    ) THEN
        RAISE EXCEPTION 'business_worker_link.business_id % is a worker listing, not a dealer', NEW.business_id
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_business_worker_link_targets
    BEFORE INSERT OR UPDATE OF business_id, provider_id ON business_worker_link
    FOR EACH ROW EXECUTE FUNCTION enforce_business_worker_link_targets();

-- ---------------------------------------------------------------------------------------------
-- Dealer reviews (user submitted, moderated)
-- ---------------------------------------------------------------------------------------------
-- Submissions start PENDING; only APPROVED reviews are public and feed business.avg_rating /
-- business.total_ratings, which are recomputed server-side on moderation and are never
-- client-settable.
CREATE TABLE business_review (
    id                             BIGSERIAL PRIMARY KEY,
    business_id                    BIGINT       NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    user_id                        BIGINT       NOT NULL REFERENCES users (id),
    reviewer_name                  VARCHAR(255) NOT NULL,
    reviewer_location              VARCHAR(120),
    rating                         SMALLINT     NOT NULL,
    review_text                    TEXT         NOT NULL,
    moderation_status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    moderation_note                TEXT,
    moderated_by_dashboard_user_id BIGINT,
    moderated_at                   TIMESTAMPTZ,
    deleted                        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_business_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_business_review_status CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_business_review_text CHECK (length(btrim(review_text)) > 0)
);

-- One active review per user per dealer.
CREATE UNIQUE INDEX uk_business_review_user_business
    ON business_review (business_id, user_id) WHERE deleted = FALSE;
CREATE INDEX idx_business_review_public
    ON business_review (business_id, moderation_status, created_at DESC, id DESC) WHERE deleted = FALSE;
CREATE INDEX idx_business_review_moderation_queue
    ON business_review (moderation_status, created_at, id) WHERE deleted = FALSE;

-- Similar-store lookups filter by city + category among active rows.
CREATE INDEX IF NOT EXISTS idx_business_city_category_active
    ON business (city_id, category_id) WHERE is_active = TRUE;
