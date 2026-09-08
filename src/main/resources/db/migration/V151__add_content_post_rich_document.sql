ALTER TABLE content_post
    ADD COLUMN content_document JSONB NOT NULL
        DEFAULT '{"schemaVersion":1,"blocks":[]}'::jsonb,
    ADD COLUMN content_document_schema_version SMALLINT NOT NULL DEFAULT 1;

ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_document_schema_version
        CHECK (content_document_schema_version = 1),
    ADD CONSTRAINT chk_content_post_document_root
        CHECK (
            jsonb_typeof(content_document) = 'object'
            AND content_document ? 'schemaVersion'
            AND content_document ? 'blocks'
            AND jsonb_typeof(content_document -> 'blocks') = 'array'
            AND (content_document ->> 'schemaVersion')::smallint
                = content_document_schema_version
        );
