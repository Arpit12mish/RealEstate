-- =========================================================
-- V82__create_public_review_tables.sql
-- Public Review Signal foundation
-- First source: Google Places reviews
-- Supports both PROJECT and BUILDER targets.
-- =========================================================

CREATE TABLE IF NOT EXISTS public_review_place (
    id BIGSERIAL PRIMARY KEY,

    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,

    source_type VARCHAR(40) NOT NULL DEFAULT 'GOOGLE_PLACES',

    google_place_id VARCHAR(255) NOT NULL,
    place_name VARCHAR(255),
    formatted_address TEXT,
    google_maps_uri TEXT,

    place_category VARCHAR(60) NOT NULL DEFAULT 'OTHER',

    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    last_synced_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_public_review_place_target_type
        CHECK (target_type IN ('PROJECT', 'BUILDER')),

    CONSTRAINT chk_public_review_place_source_type
        CHECK (source_type IN ('GOOGLE_PLACES')),

    CONSTRAINT chk_public_review_place_category
        CHECK (place_category IN (
            'PROJECT_SITE',
            'BUILDER_OFFICE',
            'SALES_OFFICE',
            'SOCIETY',
            'CLUBHOUSE',
            'OTHER'
        )),

    CONSTRAINT uq_public_review_place_target_google
        UNIQUE (target_type, target_id, google_place_id)
);

CREATE INDEX IF NOT EXISTS idx_public_review_place_target
    ON public_review_place (target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_public_review_place_google_place_id
    ON public_review_place (google_place_id);

CREATE INDEX IF NOT EXISTS idx_public_review_place_active_lookup
    ON public_review_place (target_type, target_id, active, deleted);


-- =========================================================
-- Public Review Summary
-- One summary per review place.
-- Stores aggregate rating and sample sentiment count.
-- =========================================================

CREATE TABLE IF NOT EXISTS public_review_summary (
    id BIGSERIAL PRIMARY KEY,

    review_place_id BIGINT NOT NULL,

    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,

    source_type VARCHAR(40) NOT NULL DEFAULT 'GOOGLE_PLACES',

    rating NUMERIC(2,1),
    user_rating_count INT,

    positive_sample_count INT NOT NULL DEFAULT 0,
    negative_sample_count INT NOT NULL DEFAULT 0,
    neutral_sample_count INT NOT NULL DEFAULT 0,
    mixed_sample_count INT NOT NULL DEFAULT 0,

    source_label VARCHAR(100) NOT NULL DEFAULT 'Google Maps',

    disclaimer TEXT,

    last_synced_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_public_review_summary_place
        FOREIGN KEY (review_place_id)
        REFERENCES public_review_place(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_public_review_summary_target_type
        CHECK (target_type IN ('PROJECT', 'BUILDER')),

    CONSTRAINT chk_public_review_summary_source_type
        CHECK (source_type IN ('GOOGLE_PLACES')),

    CONSTRAINT ck_public_review_summary_rating
        CHECK (rating IS NULL OR rating BETWEEN 0 AND 5),

    CONSTRAINT ck_public_review_summary_user_rating_count
        CHECK (user_rating_count IS NULL OR user_rating_count >= 0),

    CONSTRAINT uq_public_review_summary_place
        UNIQUE (review_place_id)
);

CREATE INDEX IF NOT EXISTS idx_public_review_summary_target
    ON public_review_summary (target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_public_review_summary_place
    ON public_review_summary (review_place_id);


-- =========================================================
-- Public Review Sample
-- Stores review messages returned by Google Places API.
-- These are samples, not all Google reviews.
-- =========================================================

CREATE TABLE IF NOT EXISTS public_review_sample (
    id BIGSERIAL PRIMARY KEY,

    review_place_id BIGINT NOT NULL,

    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,

    source_type VARCHAR(40) NOT NULL DEFAULT 'GOOGLE_PLACES',

    reviewer_name VARCHAR(255),
    reviewer_profile_url TEXT,
    reviewer_photo_url TEXT,

    rating INT,

    review_text TEXT,
    original_review_text TEXT,

    language_code VARCHAR(20),
    relative_publish_time VARCHAR(100),
    publish_time TIMESTAMPTZ,

    sentiment VARCHAR(30),
    category VARCHAR(80),

    display_status VARCHAR(30) NOT NULL DEFAULT 'INTERNAL_ONLY',

    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_public_review_sample_place
        FOREIGN KEY (review_place_id)
        REFERENCES public_review_place(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_public_review_sample_target_type
        CHECK (target_type IN ('PROJECT', 'BUILDER')),

    CONSTRAINT chk_public_review_sample_source_type
        CHECK (source_type IN ('GOOGLE_PLACES')),

    CONSTRAINT ck_public_review_sample_rating
        CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),

    CONSTRAINT chk_public_review_sample_sentiment
        CHECK (sentiment IS NULL OR sentiment IN (
            'POSITIVE',
            'NEGATIVE',
            'NEUTRAL',
            'MIXED'
        )),

    CONSTRAINT chk_public_review_sample_display_status
        CHECK (display_status IN (
            'INTERNAL_ONLY',
            'APPROVED_PUBLIC',
            'HIDDEN'
        ))
);

CREATE INDEX IF NOT EXISTS idx_public_review_sample_target
    ON public_review_sample (target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_public_review_sample_place
    ON public_review_sample (review_place_id);

CREATE INDEX IF NOT EXISTS idx_public_review_sample_public_lookup
    ON public_review_sample (review_place_id, display_status, rating);

CREATE INDEX IF NOT EXISTS idx_public_review_sample_rating
    ON public_review_sample (rating);

CREATE INDEX IF NOT EXISTS idx_public_review_sample_sentiment
    ON public_review_sample (sentiment);