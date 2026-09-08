ALTER TABLE mobile_app_update_policy
    ADD COLUMN policy_state varchar(16) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN store_availability varchar(24) NOT NULL DEFAULT 'UNVERIFIED',
    ADD COLUMN availability_verified_at timestamptz,
    ADD COLUMN availability_verified_by bigint,
    ADD COLUMN enforcement_mode varchar(32) NOT NULL DEFAULT 'OFF',
    ADD COLUMN emergency_disabled boolean NOT NULL DEFAULT false;

UPDATE mobile_app_update_policy
SET policy_state = CASE WHEN active THEN 'ACTIVE' ELSE 'DRAFT' END;

ALTER TABLE mobile_app_update_policy
    DROP CONSTRAINT ck_mobile_app_update_policy_active_url,
    DROP COLUMN active,
    ADD CONSTRAINT ck_mobile_app_update_policy_state
        CHECK (policy_state IN ('DRAFT', 'ACTIVE', 'SUSPENDED')),
    ADD CONSTRAINT ck_mobile_app_update_policy_availability
        CHECK (store_availability IN ('UNVERIFIED', 'PARTIAL', 'FULLY_AVAILABLE')),
    ADD CONSTRAINT ck_mobile_app_update_policy_enforcement
        CHECK (enforcement_mode IN ('OFF', 'OBSERVE', 'PROMPT_ONLY', 'ENFORCE_REPORTED_BUILDS')),
    ADD CONSTRAINT ck_mobile_app_update_policy_activation
        CHECK (policy_state <> 'ACTIVE' OR (
            store_availability = 'FULLY_AVAILABLE'
            AND availability_verified_at IS NOT NULL
            AND availability_verified_by IS NOT NULL
            AND latest_build > 0
            AND latest_version <> '0.0.0'
        ));

CREATE TABLE mobile_app_update_policy_audit (
    id bigserial PRIMARY KEY,
    platform varchar(16) NOT NULL,
    action varchar(32) NOT NULL,
    previous_policy jsonb NOT NULL,
    new_policy jsonb NOT NULL,
    dashboard_user_id bigint NOT NULL,
    dashboard_user_name varchar(150) NOT NULL,
    request_id varchar(80),
    change_reason varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mobile_app_update_policy_audit_platform
        CHECK (platform IN ('ANDROID', 'IOS')),
    CONSTRAINT ck_mobile_app_update_policy_audit_action
        CHECK (action IN ('CREATED', 'UPDATED', 'ACTIVATED', 'MINIMUM_RAISED', 'DEACTIVATED', 'ROLLED_BACK'))
);

CREATE INDEX idx_mobile_app_update_policy_audit_platform_created
    ON mobile_app_update_policy_audit (platform, created_at DESC);

