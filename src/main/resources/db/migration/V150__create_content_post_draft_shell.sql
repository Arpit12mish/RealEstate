CREATE TABLE content_post (
    id                                  BIGSERIAL PRIMARY KEY,
    content_type                        VARCHAR(20)  NOT NULL,
    status                              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    title                               VARCHAR(220) NOT NULL,
    slug                                VARCHAR(180) NOT NULL,
    excerpt                             VARCHAR(500),
    content_owner_dashboard_user_id     BIGINT       NOT NULL,
    created_by_dashboard_user_id        BIGINT       NOT NULL,
    updated_by_dashboard_user_id        BIGINT       NOT NULL,
    seo_title                           VARCHAR(200),
    seo_description                     VARCHAR(500),
    canonical_url                       VARCHAR(2048),
    robots_index                        BOOLEAN      NOT NULL DEFAULT TRUE,
    robots_follow                       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                          TIMESTAMPTZ  NOT NULL,
    updated_at                          TIMESTAMPTZ  NOT NULL,
    version                             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_content_post_slug UNIQUE (slug),
    CONSTRAINT chk_content_post_type CHECK (
        content_type IN ('ARTICLE', 'BLOG', 'INTERVIEW')
    ),
    CONSTRAINT chk_content_post_status CHECK (
        status IN ('DRAFT', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT chk_content_post_title CHECK (
        char_length(btrim(title)) BETWEEN 3 AND 220
        AND title !~ '[[:cntrl:]]'
    ),
    CONSTRAINT chk_content_post_slug CHECK (
        char_length(slug) BETWEEN 3 AND 180
        AND slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    ),
    CONSTRAINT chk_content_post_excerpt CHECK (
        excerpt IS NULL OR excerpt !~ '[[:cntrl:]]'
    ),
    CONSTRAINT chk_content_post_seo_title CHECK (
        seo_title IS NULL OR seo_title !~ '[[:cntrl:]]'
    ),
    CONSTRAINT chk_content_post_seo_description CHECK (
        seo_description IS NULL OR seo_description !~ '[[:cntrl:]]'
    ),
    CONSTRAINT chk_content_post_canonical_url CHECK (
        canonical_url IS NULL OR canonical_url ~* '^https://'
    ),
    CONSTRAINT chk_content_post_version CHECK (version >= 0),
    CONSTRAINT fk_content_post_owner_dashboard_user FOREIGN KEY (content_owner_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_post_created_by_dashboard_user FOREIGN KEY (created_by_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_post_updated_by_dashboard_user FOREIGN KEY (updated_by_dashboard_user_id)
        REFERENCES dashboard_users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_content_post_status_updated
    ON content_post (status, updated_at DESC);

CREATE INDEX idx_content_post_type_status_updated
    ON content_post (content_type, status, updated_at DESC);

CREATE INDEX idx_content_post_owner_status_updated
    ON content_post (content_owner_dashboard_user_id, status, updated_at DESC);
