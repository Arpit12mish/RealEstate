-- Durable WordPress-attachment-ID -> CMS-media-asset-ID mapping for the one-time WordPress
-- media migration. Filename/URL/post-ID alone are never used as the idempotency key (WordPress
-- attachment IDs are stable; filenames and URLs are not). cms_media_asset_id is nullable so a
-- failed attempt can still be recorded (with a safe error code) without ever having created a
-- media row - a row is only COMPLETED once a real CmsMediaAssetEntity has been verified READY.
-- Never stores image bytes, WordPress post bodies, credentials, or full exception traces.

CREATE TABLE wordpress_migration_media_mapping (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(40) NOT NULL,
    wordpress_attachment_id BIGINT NOT NULL,
    cms_media_asset_id BIGINT REFERENCES cms_media_asset (id) ON DELETE RESTRICT,
    source_upload_path VARCHAR(1024) NOT NULL,
    source_sha256 VARCHAR(64) NOT NULL,
    source_size_bytes BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    migration_state VARCHAR(30) NOT NULL,
    error_code VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_wordpress_migration_media_mapping_source
        UNIQUE (source_system, wordpress_attachment_id),
    CONSTRAINT chk_wordpress_migration_media_mapping_state
        CHECK (migration_state IN ('COMPLETED', 'FAILED')),
    CONSTRAINT chk_wordpress_migration_media_mapping_completed_has_asset
        CHECK (migration_state <> 'COMPLETED' OR cms_media_asset_id IS NOT NULL)
);

CREATE INDEX idx_wordpress_migration_media_mapping_asset
    ON wordpress_migration_media_mapping (cms_media_asset_id);
