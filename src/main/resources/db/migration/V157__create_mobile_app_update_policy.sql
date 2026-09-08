CREATE TABLE mobile_app_update_policy (
    platform varchar(16) PRIMARY KEY,
    latest_version varchar(40) NOT NULL,
    latest_build bigint NOT NULL,
    minimum_supported_build bigint NOT NULL,
    store_url varchar(500) NOT NULL,
    title varchar(120) NOT NULL,
    message varchar(500) NOT NULL,
    release_notes text,
    remind_after_hours integer NOT NULL DEFAULT 24,
    active boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_mobile_app_update_policy_platform
        CHECK (platform IN ('ANDROID', 'IOS')),
    CONSTRAINT ck_mobile_app_update_policy_latest_build
        CHECK (latest_build BETWEEN 0 AND 9007199254740991),
    CONSTRAINT ck_mobile_app_update_policy_minimum_build
        CHECK (minimum_supported_build BETWEEN 0 AND 9007199254740991),
    CONSTRAINT ck_mobile_app_update_policy_build_order
        CHECK (minimum_supported_build <= latest_build),
    CONSTRAINT ck_mobile_app_update_policy_reminder
        CHECK (remind_after_hours BETWEEN 1 AND 720),
    CONSTRAINT ck_mobile_app_update_policy_active_url
        CHECK (NOT active OR (length(trim(store_url)) > 0 AND store_url ~ '^https://'))
);

-- Safe bootstrap defaults: known store listings, but no release is active and
-- no installed build can be classified as OPTIONAL or REQUIRED.
INSERT INTO mobile_app_update_policy (
    platform, latest_version, latest_build, minimum_supported_build,
    store_url, title, message, release_notes, remind_after_hours, active
) VALUES
    ('ANDROID', '0.0.0', 0, 0,
     'https://play.google.com/store/apps/details?id=com.squarefootstory.app',
     'Update available',
     'Update Square Foot Story to get the latest improvements.',
     NULL, 24, false),
    ('IOS', '0.0.0', 0, 0,
     'https://apps.apple.com/app/id6764284866',
     'Update available',
     'Update Square Foot Story to get the latest improvements.',
     NULL, 24, false)
ON CONFLICT (platform) DO NOTHING;
