-- ContentDocument.CURRENT_SCHEMA_VERSION was bumped to 4 (LAYOUT blocks +
-- TABLE.title, see ContentDocument.java) but the check constraints added in
-- V156 were never updated to allow it, so every new content_post create
-- (which always writes CURRENT_SCHEMA_VERSION) violated
-- chk_content_post_document_schema_version and failed with a misleading
-- slug-conflict error (ContentPostServiceImpl.create() maps ANY
-- DataIntegrityViolationException on insert to a slug conflict).

ALTER TABLE content_post
    DROP CONSTRAINT chk_content_post_document_schema_version;

ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_document_schema_version
        CHECK (content_document_schema_version IN (1, 2, 3, 4));

ALTER TABLE content_post
    ALTER COLUMN content_document_schema_version SET DEFAULT 4;

ALTER TABLE content_post_revision
    DROP CONSTRAINT chk_content_revision_document_version;

ALTER TABLE content_post_revision
    ADD CONSTRAINT chk_content_revision_document_version CHECK (
        content_document_schema_version IN (1, 2, 3, 4)
        AND (content_document ->> 'schemaVersion')::smallint = content_document_schema_version
    );
