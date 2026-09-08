ALTER TABLE content_post
    ALTER COLUMN content_document SET DEFAULT '{"schemaVersion":2,"blocks":[]}'::jsonb,
    ALTER COLUMN content_document_schema_version SET DEFAULT 2;

ALTER TABLE content_post
    DROP CONSTRAINT chk_content_post_document_schema_version;

ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_document_schema_version
        CHECK (content_document_schema_version IN (1, 2));
