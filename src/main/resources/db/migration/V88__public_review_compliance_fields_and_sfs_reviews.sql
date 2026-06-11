-- ============================================================
-- V88: Public Review Compliance Fields + SFS Native Reviews
-- ============================================================

-- Add one-time-fetch compliance fields to public_review_place
ALTER TABLE public_review_place
    ADD COLUMN IF NOT EXISTS one_time_fetched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fetch_status     VARCHAR(30) NOT NULL DEFAULT 'NOT_FETCHED',
    ADD COLUMN IF NOT EXISTS content_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS display_mode     VARCHAR(30) NOT NULL DEFAULT 'HIDDEN',
    ADD COLUMN IF NOT EXISTS display_google_reviews BOOLEAN NOT NULL DEFAULT FALSE;

-- Supporting index for fetch-status queries
CREATE INDEX IF NOT EXISTS idx_public_review_place_target_fetch_status
    ON public_review_place (target_type, target_id, fetch_status);

-- ============================================================
-- SFS Native Project Reviews
-- ============================================================
CREATE TABLE IF NOT EXISTS project_review (
    id                          BIGSERIAL PRIMARY KEY,
    project_id                  BIGINT NOT NULL REFERENCES project (id),
    reviewer_name               VARCHAR(255),
    reviewer_phone_hash         VARCHAR(255),
    rating                      INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    headline                    VARCHAR(300),
    review_text                 TEXT,
    source_type                 VARCHAR(60)  NOT NULL DEFAULT 'USER_SUBMITTED',
    verification_status         VARCHAR(40)  NOT NULL DEFAULT 'PENDING',
    category                    VARCHAR(80),
    sentiment                   VARCHAR(30),
    is_featured                 BOOLEAN      NOT NULL DEFAULT FALSE,
    display_status              VARCHAR(30)  NOT NULL DEFAULT 'INTERNAL_ONLY',
    reviewed_by_dashboard_user_id BIGINT,
    reviewed_at                 TIMESTAMPTZ,
    internal_note               TEXT,
    deleted                     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_project_review_project_id
    ON project_review (project_id);

CREATE INDEX IF NOT EXISTS idx_project_review_project_status
    ON project_review (project_id, verification_status);

CREATE INDEX IF NOT EXISTS idx_project_review_display_status
    ON project_review (display_status);
