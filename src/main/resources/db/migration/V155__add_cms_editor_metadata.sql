CREATE TABLE cms_public_author (
    id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    bio VARCHAR(2000),
    designation VARCHAR(150),
    profile_media_asset_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cms_public_author_slug UNIQUE (slug),
    CONSTRAINT fk_cms_public_author_profile_media FOREIGN KEY (profile_media_asset_id)
        REFERENCES cms_media_asset(id) ON DELETE RESTRICT
);

CREATE INDEX idx_cms_public_author_active_name ON cms_public_author(active, display_name, id);

CREATE TABLE cms_content_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    normalized_name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cms_content_category_slug UNIQUE (slug),
    CONSTRAINT uk_cms_content_category_name_ci UNIQUE (normalized_name)
);

CREATE INDEX idx_cms_content_category_active_name ON cms_content_category(active, name, id);

CREATE TABLE cms_content_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cms_content_tag_slug UNIQUE (slug),
    CONSTRAINT uk_cms_content_tag_name_ci UNIQUE (normalized_name)
);

CREATE INDEX idx_cms_content_tag_active_name ON cms_content_tag(active, name, id);

ALTER TABLE content_post
    ADD COLUMN public_author_id BIGINT,
    ADD COLUMN category_id BIGINT,
    ADD COLUMN cover_media_asset_id BIGINT,
    ADD COLUMN cover_alt_text VARCHAR(300),
    ADD CONSTRAINT fk_content_post_public_author FOREIGN KEY (public_author_id)
        REFERENCES cms_public_author(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_post_category FOREIGN KEY (category_id)
        REFERENCES cms_content_category(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_post_cover_media FOREIGN KEY (cover_media_asset_id)
        REFERENCES cms_media_asset(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_content_post_cover_alt CHECK (
        cover_alt_text IS NULL OR char_length(btrim(cover_alt_text)) BETWEEN 1 AND 300
    );

CREATE INDEX idx_content_post_public_author ON content_post(public_author_id);
CREATE INDEX idx_content_post_category ON content_post(category_id);

CREATE TABLE content_post_tag (
    content_post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT uk_content_post_tag PRIMARY KEY(content_post_id, tag_id),
    CONSTRAINT fk_content_post_tag_post FOREIGN KEY(content_post_id)
        REFERENCES content_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_post_tag_tag FOREIGN KEY(tag_id)
        REFERENCES cms_content_tag(id) ON DELETE RESTRICT
);
CREATE INDEX idx_content_post_tag_tag ON content_post_tag(tag_id, content_post_id);

ALTER TABLE content_post_revision
    ADD COLUMN public_author_id BIGINT,
    ADD COLUMN public_author_name VARCHAR(150),
    ADD COLUMN public_author_slug VARCHAR(180),
    ADD COLUMN public_author_designation VARCHAR(150),
    ADD COLUMN public_author_profile_media_asset_id BIGINT,
    ADD COLUMN category_id BIGINT,
    ADD COLUMN category_name VARCHAR(150),
    ADD COLUMN category_slug VARCHAR(180),
    ADD COLUMN tag_snapshots JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN cover_media_asset_id BIGINT,
    ADD COLUMN cover_alt_text VARCHAR(300),
    ADD CONSTRAINT fk_content_revision_public_author FOREIGN KEY(public_author_id)
        REFERENCES cms_public_author(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_revision_category FOREIGN KEY(category_id)
        REFERENCES cms_content_category(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_revision_author_profile_media FOREIGN KEY(public_author_profile_media_asset_id)
        REFERENCES cms_media_asset(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_content_revision_cover_media FOREIGN KEY(cover_media_asset_id)
        REFERENCES cms_media_asset(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_content_revision_tags_array CHECK (jsonb_typeof(tag_snapshots) = 'array'),
    ADD CONSTRAINT chk_content_revision_cover_alt CHECK (
        cover_alt_text IS NULL OR char_length(btrim(cover_alt_text)) BETWEEN 1 AND 300
    );

CREATE INDEX idx_content_revision_category ON content_post_revision(category_id, content_post_id);
CREATE INDEX idx_content_revision_author ON content_post_revision(public_author_id, content_post_id);
CREATE INDEX idx_content_revision_tags_gin ON content_post_revision USING GIN(tag_snapshots jsonb_path_ops);

CREATE FUNCTION cms_revision_has_tag(tags JSONB, requested_slug VARCHAR)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT tags @> jsonb_build_array(jsonb_build_object('slug', requested_slug))
$$;

-- Hibernate binds a nullable function argument as bytea when all filters are null.
-- This overload keeps the prepared query stable while preserving exact slug matching.
CREATE FUNCTION cms_revision_has_tag(tags JSONB, requested_slug BYTEA)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT requested_slug IS NOT NULL AND tags @> jsonb_build_array(
        jsonb_build_object('slug', convert_from(requested_slug, 'UTF8'))
    )
$$;
