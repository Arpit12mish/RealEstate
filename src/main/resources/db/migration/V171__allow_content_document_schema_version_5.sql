-- ContentDocument.CURRENT_SCHEMA_VERSION is bumped to 5 to add the IMAGE_GALLERY block
-- (see ContentBlock.Gallery). Existing schemaVersion 1-4 documents are unaffected and remain
-- readable as-is; only the CHECK constraints enumerating allowed versions are relaxed,
-- matching the V153/V156/V164 precedent.

ALTER TABLE content_post
    DROP CONSTRAINT chk_content_post_document_schema_version;

ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_document_schema_version
        CHECK (content_document_schema_version IN (1, 2, 3, 4, 5));

ALTER TABLE content_post
    ALTER COLUMN content_document_schema_version SET DEFAULT 5;

ALTER TABLE content_post_revision
    DROP CONSTRAINT chk_content_revision_document_version;

ALTER TABLE content_post_revision
    ADD CONSTRAINT chk_content_revision_document_version CHECK (
        content_document_schema_version IN (1, 2, 3, 4, 5)
        AND (content_document ->> 'schemaVersion')::smallint = content_document_schema_version
    );
