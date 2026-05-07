-- =========================================================
-- V66__create_dashboard_auth_and_review_tables.sql
-- Dashboard authentication + review foundation
-- =========================================================

-- =========================
-- 1. Dashboard Users
-- =========================
CREATE TABLE dashboard_users (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    last_login_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_dashboard_users_role
        CHECK (role IN ('ADMIN', 'REVIEWER', 'DATA_ENTRY'))
);

CREATE INDEX idx_dashboard_users_email
    ON dashboard_users (email);

CREATE INDEX idx_dashboard_users_role
    ON dashboard_users (role);

CREATE INDEX idx_dashboard_users_active
    ON dashboard_users (active);


-- =========================
-- 2. Dashboard Refresh Tokens
-- =========================
CREATE TABLE dashboard_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,

    dashboard_user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    revoked_at TIMESTAMP WITH TIME ZONE,

    replaced_by_token_hash VARCHAR(255),

    CONSTRAINT fk_dashboard_refresh_tokens_user
        FOREIGN KEY (dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_dashboard_refresh_tokens_user_id
    ON dashboard_refresh_tokens (dashboard_user_id);

CREATE INDEX idx_dashboard_refresh_tokens_token_hash
    ON dashboard_refresh_tokens (token_hash);

CREATE INDEX idx_dashboard_refresh_tokens_revoked
    ON dashboard_refresh_tokens (revoked);

CREATE INDEX idx_dashboard_refresh_tokens_expires_at
    ON dashboard_refresh_tokens (expires_at);


-- =========================
-- 3. Dashboard Login Audit
-- =========================
CREATE TABLE dashboard_login_audit (
    id BIGSERIAL PRIMARY KEY,

    dashboard_user_id BIGINT,
    email VARCHAR(180),

    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),

    ip_address VARCHAR(100),
    user_agent TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_dashboard_login_audit_user
        FOREIGN KEY (dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_dashboard_login_audit_user_id
    ON dashboard_login_audit (dashboard_user_id);

CREATE INDEX idx_dashboard_login_audit_email
    ON dashboard_login_audit (email);

CREATE INDEX idx_dashboard_login_audit_success
    ON dashboard_login_audit (success);

CREATE INDEX idx_dashboard_login_audit_created_at
    ON dashboard_login_audit (created_at);


-- =========================
-- 4. Dashboard Content Review History
-- =========================
CREATE TABLE dashboard_content_review_history (
    id BIGSERIAL PRIMARY KEY,

    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,

    old_status VARCHAR(50),
    new_status VARCHAR(50),

    action_type VARCHAR(80) NOT NULL,
    remarks TEXT,

    action_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_dashboard_review_history_action_by
        FOREIGN KEY (action_by)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_dashboard_review_history_entity_type
        CHECK (
            entity_type IN (
                'PROJECT',
                'PROJECT_MEDIA',
                'PROJECT_FLOOR_PLAN',
                'PROJECT_HIGHLIGHT',
                'PROJECT_CONNECTIVITY',
                'PROJECT_CONNECTIVITY_PLACE',
                'PROJECT_METER',
                'PROJECT_METER_SNAPSHOT',
                'BUILDER',
                'COMPANY',
                'PROMO_BANNER',
                'APP_CONTENT'
            )
        ),

    CONSTRAINT chk_dashboard_review_history_action_type
        CHECK (
            action_type IN (
                'CREATED',
                'UPDATED',
                'SUBMITTED_FOR_REVIEW',
                'FIELD_MARKED_RECHECK',
                'FIELD_MARKED_WRONG',
                'FIELD_MARKED_FIXED',
                'APPROVED',
                'REJECTED',
                'MOVED_TO_RECHECK',
                'PUBLISHED',
                'UNPUBLISHED',
                'ARCHIVED',
                'RESTORED'
            )
        )
);

CREATE INDEX idx_dashboard_review_history_entity
    ON dashboard_content_review_history (entity_type, entity_id);

CREATE INDEX idx_dashboard_review_history_action_by
    ON dashboard_content_review_history (action_by);

CREATE INDEX idx_dashboard_review_history_action_type
    ON dashboard_content_review_history (action_type);

CREATE INDEX idx_dashboard_review_history_created_at
    ON dashboard_content_review_history (created_at);


-- =========================
-- 5. Dashboard Field Review Issues
-- =========================
CREATE TABLE dashboard_field_review_issues (
    id BIGSERIAL PRIMARY KEY,

    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,

    field_key VARCHAR(120) NOT NULL,
    field_label VARCHAR(180),

    status VARCHAR(40) NOT NULL,
    remarks TEXT,

    marked_by BIGINT NOT NULL,
    fixed_by BIGINT,

    marked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    fixed_at TIMESTAMP WITH TIME ZONE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_dashboard_field_review_marked_by
        FOREIGN KEY (marked_by)
        REFERENCES dashboard_users (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_dashboard_field_review_fixed_by
        FOREIGN KEY (fixed_by)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL,

    CONSTRAINT chk_dashboard_field_review_entity_type
        CHECK (
            entity_type IN (
                'PROJECT',
                'PROJECT_MEDIA',
                'PROJECT_FLOOR_PLAN',
                'PROJECT_HIGHLIGHT',
                'PROJECT_CONNECTIVITY',
                'PROJECT_CONNECTIVITY_PLACE',
                'PROJECT_METER',
                'PROJECT_METER_SNAPSHOT',
                'BUILDER',
                'COMPANY',
                'PROMO_BANNER',
                'APP_CONTENT'
            )
        ),

    CONSTRAINT chk_dashboard_field_review_status
        CHECK (status IN ('RECHECK', 'WRONG', 'FIXED'))
);

CREATE INDEX idx_dashboard_field_review_entity
    ON dashboard_field_review_issues (entity_type, entity_id);

CREATE INDEX idx_dashboard_field_review_field_key
    ON dashboard_field_review_issues (field_key);

CREATE INDEX idx_dashboard_field_review_status
    ON dashboard_field_review_issues (status);

CREATE INDEX idx_dashboard_field_review_active
    ON dashboard_field_review_issues (active);

CREATE INDEX idx_dashboard_field_review_marked_by
    ON dashboard_field_review_issues (marked_by);

CREATE INDEX idx_dashboard_field_review_fixed_by
    ON dashboard_field_review_issues (fixed_by);


-- Prevent duplicate active issue for same entity + same field.
-- Example: one active WRONG issue for PROJECT 10 reraNumber.
CREATE UNIQUE INDEX uq_dashboard_active_field_issue
    ON dashboard_field_review_issues (entity_type, entity_id, field_key)
    WHERE active = TRUE;