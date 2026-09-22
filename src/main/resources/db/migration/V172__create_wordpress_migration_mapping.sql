-- Durable provenance/idempotency record for the one-time WordPress-to-CMS migration. The CMS
-- has no existing external-source mapping mechanism, so this is the smallest dedicated table
-- needed to associate a source WordPress post with its (optional) target content_post row and
-- detect whether the source has changed since the last import - slug alone is never used as the
-- idempotency key (a slug can be regenerated or edited after import). Never stores the WordPress
-- post body itself - source_fingerprint is a hash, not the content.

CREATE TABLE wordpress_migration_mapping (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(40) NOT NULL,
    source_post_id BIGINT NOT NULL,
    target_content_id BIGINT REFERENCES content_post (id) ON DELETE SET NULL,
    source_fingerprint VARCHAR(64) NOT NULL,
    migration_state VARCHAR(30) NOT NULL,
    error_code VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_wordpress_migration_mapping_source UNIQUE (source_system, source_post_id),
    CONSTRAINT chk_wordpress_migration_mapping_state
        CHECK (migration_state IN ('PUBLISHED', 'DRAFT', 'NEEDS_REVIEW', 'BLOCKED', 'FAILED'))
);

CREATE INDEX idx_wordpress_migration_mapping_target ON wordpress_migration_mapping (target_content_id);
