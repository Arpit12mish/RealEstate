CREATE TABLE cms_media_asset (
    id BIGSERIAL PRIMARY KEY,
    media_type VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    storage_bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    declared_size_bytes BIGINT NOT NULL,
    size_bytes BIGINT,
    width INTEGER,
    height INTEGER,
    duration_millis BIGINT,
    object_etag VARCHAR(128),
    created_by_dashboard_user_id BIGINT NOT NULL,
    ready_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cms_media_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_cms_media_creator_dashboard_user
        FOREIGN KEY (created_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cms_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT chk_cms_media_status CHECK (status IN ('PENDING_UPLOAD', 'READY', 'FAILED', 'DELETING')),
    CONSTRAINT chk_cms_media_declared_size CHECK (declared_size_bytes > 0),
    CONSTRAINT chk_cms_media_size CHECK (size_bytes IS NULL OR size_bytes > 0),
    CONSTRAINT chk_cms_media_width CHECK (width IS NULL OR width > 0),
    CONSTRAINT chk_cms_media_height CHECK (height IS NULL OR height > 0),
    CONSTRAINT chk_cms_media_duration CHECK (duration_millis IS NULL OR duration_millis >= 0),
    CONSTRAINT chk_cms_media_ready_state CHECK (
        status <> 'READY' OR (size_bytes IS NOT NULL AND ready_at IS NOT NULL AND failure_code IS NULL)
    ),
    CONSTRAINT chk_cms_media_ready_image_dimensions CHECK (
        status <> 'READY' OR media_type <> 'IMAGE' OR (width IS NOT NULL AND height IS NOT NULL)
    ),
    CONSTRAINT chk_cms_media_failed_state CHECK (
        status <> 'FAILED' OR (failed_at IS NOT NULL AND failure_code IS NOT NULL)
    )
);

CREATE INDEX idx_cms_media_status_created
    ON cms_media_asset (status, created_at DESC);
CREATE INDEX idx_cms_media_type_status_created
    ON cms_media_asset (media_type, status, created_at DESC);
CREATE INDEX idx_cms_media_creator_created
    ON cms_media_asset (created_by_dashboard_user_id, created_at DESC);
