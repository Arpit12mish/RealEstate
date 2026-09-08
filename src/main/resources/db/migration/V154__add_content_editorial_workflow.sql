CREATE TABLE content_post_revision (
    id                              BIGSERIAL PRIMARY KEY,
    content_post_id                 BIGINT       NOT NULL,
    revision_number                 INTEGER      NOT NULL,
    content_type                    VARCHAR(20)  NOT NULL,
    title                           VARCHAR(220) NOT NULL,
    slug                            VARCHAR(180) NOT NULL,
    excerpt                         VARCHAR(500),
    seo_title                       VARCHAR(200),
    seo_description                 VARCHAR(500),
    canonical_url                   VARCHAR(2048),
    robots_index                    BOOLEAN      NOT NULL,
    robots_follow                   BOOLEAN      NOT NULL,
    content_document                JSONB        NOT NULL,
    content_document_schema_version SMALLINT     NOT NULL,
    created_from_post_version       BIGINT       NOT NULL,
    created_by_dashboard_user_id    BIGINT       NOT NULL,
    revision_reason                 VARCHAR(32)  NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_content_revision_post FOREIGN KEY (content_post_id)
        REFERENCES content_post (id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_revision_creator FOREIGN KEY (created_by_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_content_revision_post_number UNIQUE (content_post_id, revision_number),
    CONSTRAINT uk_content_revision_id_post UNIQUE (id, content_post_id),
    CONSTRAINT chk_content_revision_number CHECK (revision_number > 0),
    CONSTRAINT chk_content_revision_source_version CHECK (created_from_post_version >= 0),
    CONSTRAINT chk_content_revision_type CHECK (content_type IN ('ARTICLE', 'BLOG', 'INTERVIEW')),
    CONSTRAINT chk_content_revision_reason CHECK (revision_reason = 'REVIEW_SUBMISSION'),
    CONSTRAINT chk_content_revision_document_version CHECK (
        content_document_schema_version IN (1, 2)
        AND (content_document ->> 'schemaVersion')::smallint = content_document_schema_version
    )
);

CREATE INDEX idx_content_revision_post_created
    ON content_post_revision (content_post_id, created_at DESC);

CREATE TABLE content_review_activity (
    id                          BIGSERIAL PRIMARY KEY,
    content_post_id             BIGINT       NOT NULL,
    revision_id                 BIGINT,
    action                      VARCHAR(40)  NOT NULL,
    comment                     VARCHAR(4000),
    actor_dashboard_user_id     BIGINT       NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_content_review_activity_post FOREIGN KEY (content_post_id)
        REFERENCES content_post (id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_review_activity_revision FOREIGN KEY (revision_id, content_post_id)
        REFERENCES content_post_revision (id, content_post_id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_review_activity_actor FOREIGN KEY (actor_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_content_review_activity_action CHECK (action IN (
        'SUBMITTED_FOR_REVIEW', 'CHANGES_REQUESTED', 'APPROVED',
        'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED'
    )),
    CONSTRAINT chk_content_review_comment CHECK (
        comment IS NULL OR (char_length(btrim(comment)) BETWEEN 1 AND 4000)
    ),
    CONSTRAINT chk_content_changes_comment CHECK (
        action <> 'CHANGES_REQUESTED' OR comment IS NOT NULL
    )
);

CREATE INDEX idx_content_review_activity_post_created
    ON content_review_activity (content_post_id, created_at DESC);

ALTER TABLE content_post
    ADD COLUMN current_review_revision_id BIGINT,
    ADD COLUMN approved_revision_id BIGINT,
    ADD COLUMN current_published_revision_id BIGINT,
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN published_by_dashboard_user_id BIGINT;

ALTER TABLE content_post
    ADD CONSTRAINT fk_content_post_current_review_revision
        FOREIGN KEY (current_review_revision_id, id)
        REFERENCES content_post_revision (id, content_post_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_post_approved_revision
        FOREIGN KEY (approved_revision_id, id)
        REFERENCES content_post_revision (id, content_post_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_post_current_published_revision
        FOREIGN KEY (current_published_revision_id, id)
        REFERENCES content_post_revision (id, content_post_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_post_published_by_dashboard_user
        FOREIGN KEY (published_by_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT;

ALTER TABLE content_post DROP CONSTRAINT chk_content_post_status;
ALTER TABLE content_post
    ADD CONSTRAINT chk_content_post_status CHECK (status IN (
        'DRAFT', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED',
        'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED'
    )),
    ADD CONSTRAINT chk_content_post_publication_actor_time CHECK (
        (published_at IS NULL) = (published_by_dashboard_user_id IS NULL)
    ),
    ADD CONSTRAINT chk_content_post_workflow_pointers CHECK (
        (status <> 'IN_REVIEW' OR (current_review_revision_id IS NOT NULL AND approved_revision_id IS NULL))
        AND (status <> 'CHANGES_REQUESTED' OR (current_review_revision_id IS NOT NULL AND approved_revision_id IS NULL))
        AND (status <> 'APPROVED' OR (
            current_review_revision_id IS NOT NULL
            AND approved_revision_id = current_review_revision_id
        ))
        AND (status <> 'PUBLISHED' OR (
            approved_revision_id IS NOT NULL
            AND current_published_revision_id = approved_revision_id
            AND published_at IS NOT NULL
            AND published_by_dashboard_user_id IS NOT NULL
        ))
        AND (status = 'PUBLISHED' OR current_published_revision_id IS NULL)
        AND (status <> 'UNPUBLISHED' OR (
            approved_revision_id IS NOT NULL
            AND published_at IS NOT NULL
            AND published_by_dashboard_user_id IS NOT NULL
        ))
    );

CREATE OR REPLACE FUNCTION prevent_cms_immutable_row_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'CMS revision and workflow history rows are immutable'
        USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER trg_content_post_revision_immutable
    BEFORE UPDATE ON content_post_revision
    FOR EACH ROW EXECUTE FUNCTION prevent_cms_immutable_row_update();

CREATE TRIGGER trg_content_review_activity_immutable
    BEFORE UPDATE ON content_review_activity
    FOR EACH ROW EXECUTE FUNCTION prevent_cms_immutable_row_update();
