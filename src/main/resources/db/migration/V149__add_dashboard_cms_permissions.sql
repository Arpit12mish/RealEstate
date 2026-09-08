ALTER TABLE dashboard_users
    DROP CONSTRAINT IF EXISTS chk_dashboard_users_role;

ALTER TABLE dashboard_users
    ADD CONSTRAINT chk_dashboard_users_role
        CHECK (role IN ('ADMIN', 'REVIEWER', 'DATA_ENTRY', 'CONTENT_STAFF'));

CREATE TABLE dashboard_user_permissions (
    dashboard_user_id BIGINT NOT NULL,
    permission VARCHAR(80) NOT NULL,
    CONSTRAINT pk_dashboard_user_permissions
        PRIMARY KEY (dashboard_user_id, permission),
    CONSTRAINT fk_dashboard_user_permissions_user
        FOREIGN KEY (dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_dashboard_user_permissions_value
        CHECK (permission IN (
            'CMS_CONTENT_CREATE',
            'CMS_CONTENT_EDIT_OWN',
            'CMS_CONTENT_EDIT_ANY',
            'CMS_CONTENT_SUBMIT_REVIEW',
            'CMS_CONTENT_REVIEW',
            'CMS_CONTENT_PREVIEW',
            'CMS_CONTENT_PUBLISH',
            'CMS_CONTENT_UNPUBLISH',
            'CMS_CONTENT_ARCHIVE',
            'CMS_MEDIA_UPLOAD',
            'CMS_USER_MANAGE'
        ))
);

CREATE INDEX idx_dashboard_user_permissions_permission
    ON dashboard_user_permissions (permission);
