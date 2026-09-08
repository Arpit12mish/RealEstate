-- ContentDocument schemaVersion 3 adds TABLE, CALLOUT, and CHECK_LIST blocks. Existing
-- schemaVersion 1/2 documents are unaffected and remain readable as-is; only the CHECK
-- constraints enumerating allowed versions are relaxed, matching the V153 precedent that
-- did the same for schemaVersion 2.

ALTER TABLE content_post
    ALTER COLUMN content_document SET DEFAULT '{"schemaVersion":3,"blocks":[]}'::jsonb,
    ALTER COLUMN content_document_schema_version SET DEFAULT 3;

ALTER TABLE content_post
    DROP CONSTRAINT chk_content_post_document_schema_version;

ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_document_schema_version
        CHECK (content_document_schema_version IN (1, 2, 3));

ALTER TABLE content_post_revision
    DROP CONSTRAINT chk_content_revision_document_version;

ALTER TABLE content_post_revision
    ADD CONSTRAINT chk_content_revision_document_version CHECK (
        content_document_schema_version IN (1, 2, 3)
        AND (content_document ->> 'schemaVersion')::smallint = content_document_schema_version
    );
