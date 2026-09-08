package com.brandPitara.sfs.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class ForwardOnlyFlywayMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sfs_forward_migration")
                    .withUsername("sfs_test")
                    .withPassword("sfs_test");

    @BeforeEach
    void resetSchema() throws SQLException {
        try (
                Connection connection = connection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
        }
    }

    @Test
    void freshPostgresUsesBaselineThenCanonicalProductionHistoryAndForwardRepair()
            throws Exception {

        Flyway flyway = flyway(null);

        assertThat(flyway.migrate().success).isTrue();
        flyway.validate();

        assertThat(text("""
                SELECT version
                FROM flyway_schema_history
                WHERE type = 'SQL_BASELINE'
                """))
                .isEqualTo("134");

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version::integer BETWEEN 135 AND 139
                  AND success
                """))
                .as("canonical production migrations V135-V139 must execute after B134")
                .isEqualTo(5);

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '140'
                  AND description = 'repair historical schema drift'
                  AND success
                """))
                .isOne();

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '141'
                  AND description = 'restore canonical baseline reference data'
                  AND success
                """))
                .isOne();

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '142'
                  AND description = 'add company project slug unique index'
                  AND success
                """))
                .isOne();

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version::integer BETWEEN 143 AND 146
                """))
                .as("obsolete duplicate renumbered migrations V143-V146 must be absent")
                .isZero();

        assertThat(text("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """))
                .isEqualTo("156");

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE NOT success
                """))
                .isZero();

        assertThat(number("""
                SELECT count(*)
                FROM flyway_schema_history
                WHERE lower(script) LIKE '%callback%'
                """))
                .isZero();

        assertIndex("uk_company_project_slug");
        assertColumn("content_post", "version");
        assertColumnType("content_post", "content_document", "jsonb");
        assertColumnType("content_post", "content_document_schema_version", "smallint");
        assertConstraint("content_post", "u");
        assertConstraint("content_post", "f");
        assertConstraint("content_post", "c");
        assertIndex("uk_content_post_slug");
        assertIndex("idx_content_post_status_updated");
        assertIndex("idx_content_post_type_status_updated");
        assertIndex("idx_content_post_owner_status_updated");
        assertColumn("cms_media_asset", "version");
        assertIndex("uk_cms_media_storage_key");
        assertIndex("idx_cms_media_status_created");
        assertIndex("idx_cms_media_type_status_created");
        assertIndex("idx_cms_media_creator_created");

        assertThat(number("SELECT count(*) FROM category"))
                .isEqualTo(68);

        assertThat(number("SELECT count(*) FROM city"))
                .isEqualTo(11);

        assertThat(number("SELECT count(*) FROM content_version"))
                .isEqualTo(5);

        assertThat(number("""
                SELECT count(*)
                FROM home_section_config
                WHERE home_category_id = 0
                """))
                .isEqualTo(7);

        assertThat(number("""
                SELECT count(*)
                FROM promo_banner_slot_config
                WHERE screen = 'HOME'
                  AND home_category_id = 0
                  AND slot_key = 'HERO'
                """))
                .isOne();

        assertThat(number("SELECT count(*) FROM business"))
                .isZero();

        assertThat(number("SELECT count(*) FROM brand"))
                .isZero();

        assertThat(number("SELECT count(*) FROM company"))
                .isZero();

        assertThat(number("SELECT count(*) FROM promo_banner"))
                .isZero();

        assertThat(text("""
                SELECT parent.slug
                FROM category child
                JOIN category parent
                  ON parent.id = child.parent_id
                WHERE child.slug = 'plumbers'
                """))
                .isEqualTo("home-repair-maintenance");

        assertThat(text("""
                SELECT parent.slug
                FROM category child
                JOIN category parent
                  ON parent.id = child.parent_id
                WHERE child.slug = 'tiles-flooring'
                """))
                .isEqualTo("flooring-tiles");
    }

    @Test
    void cmsMediaMigrationEnforcesTrustedAssetLifecycleAndHistoricalCreator() throws Exception {
        assertThat(flyway(null).migrate().success).isTrue();
        execute("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES (9601, 'Media Writer', 'media-writer@example.com', 'encoded', 'CONTENT_STAFF', true, now(), now());
                INSERT INTO cms_media_asset (
                    media_type, status, storage_bucket, storage_key, original_filename,
                    content_type, declared_size_bytes, created_by_dashboard_user_id
                ) VALUES (
                    'IMAGE', 'PENDING_UPLOAD', 'private', 'cms/images/one.jpg', 'one.jpg',
                    'image/jpeg', 100, 9601
                );
                """);

        assertThatThrownBy(() -> execute("""
                INSERT INTO cms_media_asset (
                    media_type, status, storage_bucket, storage_key, original_filename,
                    content_type, declared_size_bytes, created_by_dashboard_user_id
                ) VALUES ('VIDEO', 'PENDING_UPLOAD', 'private', 'cms/images/one.jpg', 'one.mp4',
                          'video/mp4', 100, 9601)
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo("23505");

        assertThatThrownBy(() -> execute("""
                INSERT INTO cms_media_asset (
                    media_type, status, storage_bucket, storage_key, original_filename,
                    content_type, declared_size_bytes, created_by_dashboard_user_id
                ) VALUES ('AUDIO', 'READY', 'private', 'cms/audio/x', 'x.mp3',
                          'audio/mpeg', -1, 9601)
                """))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo("23514");

        assertThatThrownBy(() -> execute("DELETE FROM dashboard_users WHERE id = 9601"))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo("23503");
        assertThat(number("SELECT count(*) FROM cms_media_asset WHERE created_by_dashboard_user_id = 9601")).isOne();
    }

    @Test
    void contentPostMigrationEnforcesSlugDomainAndHistoricalUserReferences()
            throws Exception {

        Flyway flyway = flyway(null);
        assertThat(flyway.migrate().success).isTrue();

        execute("""
                INSERT INTO dashboard_users (
                    id, name, email, password_hash, role, active, created_at, updated_at
                ) VALUES (
                    9501, 'CMS Writer', 'cms-writer@example.com', 'encoded',
                    'CONTENT_STAFF', TRUE, now(), now()
                );

                INSERT INTO content_post (
                    id, content_type, status, title, slug,
                    content_owner_dashboard_user_id,
                    created_by_dashboard_user_id,
                    updated_by_dashboard_user_id,
                    robots_index, robots_follow, created_at, updated_at, version
                ) VALUES (
                    9501, 'ARTICLE', 'DRAFT', 'Gurgaon Market Guide',
                    'gurgaon-market-guide', 9501, 9501, 9501,
                    TRUE, TRUE, now(), now(), 0
                );
                """);

        assertThatThrownBy(() -> execute("""
                INSERT INTO content_post (
                    content_type, status, title, slug,
                    content_owner_dashboard_user_id,
                    created_by_dashboard_user_id,
                    updated_by_dashboard_user_id,
                    created_at, updated_at
                ) VALUES (
                    'BLOG', 'DRAFT', 'Another Market Guide',
                    'gurgaon-market-guide', 9501, 9501, 9501, now(), now()
                )
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23505");

        assertThatThrownBy(() -> execute("""
                INSERT INTO content_post (
                    content_type, status, title, slug,
                    content_owner_dashboard_user_id,
                    created_by_dashboard_user_id,
                    updated_by_dashboard_user_id,
                    canonical_url, created_at, updated_at
                ) VALUES (
                    'NEWS', 'PUBLISHED', 'Unsafe Content Post', 'unsafe-content-post',
                    9501, 9501, 9501, 'javascript:alert(1)', now(), now()
                )
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23514");

        assertThatThrownBy(() -> execute("DELETE FROM dashboard_users WHERE id = 9501"))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23503");

        assertThat(number("SELECT count(*) FROM content_post WHERE id = 9501"))
                .isOne();

        assertThat(text("""
                SELECT content_document ->> 'schemaVersion'
                FROM content_post
                WHERE id = 9501
                """)).isEqualTo("3");
        assertThat(text("""
                SELECT jsonb_typeof(content_document -> 'blocks')
                FROM content_post
                WHERE id = 9501
                """)).isEqualTo("array");

        assertThatThrownBy(() -> execute("""
                UPDATE content_post
                SET content_document = '{"schemaVersion":1,"blocks":[]}'::jsonb
                WHERE id = 9501
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23514");
    }

    @Test
    void documentV2MigrationPreservesStoredV1DocumentsAndAllowsBothVersions() throws Exception {
        Flyway throughV152 = flyway(MigrationVersion.fromVersion("152"));
        assertThat(throughV152.migrate().success).isTrue();
        execute("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES (9701, 'Document Writer', 'document-writer@example.com', 'encoded',
                        'CONTENT_STAFF', true, now(), now());
                INSERT INTO content_post (
                    id, content_type, status, title, slug,
                    content_owner_dashboard_user_id, created_by_dashboard_user_id,
                    updated_by_dashboard_user_id, robots_index, robots_follow,
                    content_document, content_document_schema_version,
                    created_at, updated_at, version
                ) VALUES (
                    9701, 'ARTICLE', 'DRAFT', 'Existing V1', 'existing-v1',
                    9701, 9701, 9701, true, true,
                    '{"schemaVersion":1,"blocks":[]}'::jsonb, 1,
                    now(), now(), 0
                );
                """);

        assertThat(flyway(null).migrate().success).isTrue();
        assertThat(text("SELECT content_document ->> 'schemaVersion' FROM content_post WHERE id = 9701"))
                .isEqualTo("1");
        execute("""
                UPDATE content_post
                SET content_document = '{"schemaVersion":2,"blocks":[]}'::jsonb,
                    content_document_schema_version = 2
                WHERE id = 9701
                """);
        assertThat(text("SELECT content_document ->> 'schemaVersion' FROM content_post WHERE id = 9701"))
                .isEqualTo("2");
    }

    @Test
    void documentV3MigrationPreservesStoredV2DocumentsAndAllowsAllThreeVersions() throws Exception {
        Flyway throughV155 = flyway(MigrationVersion.fromVersion("155"));
        assertThat(throughV155.migrate().success).isTrue();
        execute("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES (9702, 'Document Writer Two', 'document-writer-2@example.com', 'encoded',
                        'CONTENT_STAFF', true, now(), now());
                INSERT INTO content_post (
                    id, content_type, status, title, slug,
                    content_owner_dashboard_user_id, created_by_dashboard_user_id,
                    updated_by_dashboard_user_id, robots_index, robots_follow,
                    content_document, content_document_schema_version,
                    created_at, updated_at, version
                ) VALUES (
                    9702, 'ARTICLE', 'DRAFT', 'Existing V2', 'existing-v2',
                    9702, 9702, 9702, true, true,
                    '{"schemaVersion":2,"blocks":[]}'::jsonb, 2,
                    now(), now(), 0
                );
                """);

        assertThat(flyway(null).migrate().success).isTrue();
        assertThat(text("SELECT content_document ->> 'schemaVersion' FROM content_post WHERE id = 9702"))
                .isEqualTo("2");

        execute("""
                UPDATE content_post
                SET content_document = '{"schemaVersion":3,"blocks":[
                        {"type":"CALLOUT","variant":"VERDICT","title":"SFS Verdict",
                         "content":[{"type":"TEXT","text":"Strong pick.","marks":[]}]}
                    ]}'::jsonb,
                    content_document_schema_version = 3
                WHERE id = 9702
                """);
        assertThat(text("SELECT content_document ->> 'schemaVersion' FROM content_post WHERE id = 9702"))
                .isEqualTo("3");

        assertThatThrownBy(() -> execute("""
                UPDATE content_post
                SET content_document_schema_version = 4
                WHERE id = 9702
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23514");
    }

    @Test
    void editorialWorkflowMigrationProtectsExactImmutablePostRevisions() throws Exception {
        assertThat(flyway(null).migrate().success).isTrue();
        execute("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES
                    (9801, 'Workflow Writer', 'workflow-writer@example.com', 'encoded',
                     'CONTENT_STAFF', true, now(), now()),
                    (9802, 'Workflow Editor', 'workflow-editor@example.com', 'encoded',
                     'CONTENT_STAFF', true, now(), now());

                INSERT INTO content_post (
                    id, content_type, status, title, slug,
                    content_owner_dashboard_user_id, created_by_dashboard_user_id,
                    updated_by_dashboard_user_id, robots_index, robots_follow,
                    content_document, content_document_schema_version,
                    created_at, updated_at, version
                ) VALUES
                    (9801, 'ARTICLE', 'DRAFT', 'Workflow Post', 'workflow-post',
                     9801, 9801, 9801, true, true,
                     '{"schemaVersion":2,"blocks":[]}'::jsonb, 2, now(), now(), 0),
                    (9802, 'BLOG', 'DRAFT', 'Other Post', 'other-workflow-post',
                     9802, 9802, 9802, true, true,
                     '{"schemaVersion":2,"blocks":[]}'::jsonb, 2, now(), now(), 0);

                INSERT INTO content_post_revision (
                    id, content_post_id, revision_number, content_type, title, slug,
                    robots_index, robots_follow, content_document,
                    content_document_schema_version, created_from_post_version,
                    created_by_dashboard_user_id, revision_reason, created_at
                ) VALUES (
                    9801, 9801, 1, 'ARTICLE', 'Reviewed title', 'workflow-post',
                    true, true, '{"schemaVersion":2,"blocks":[]}'::jsonb,
                    2, 0, 9801, 'REVIEW_SUBMISSION', now()
                );

                UPDATE content_post
                SET status = 'IN_REVIEW', current_review_revision_id = 9801
                WHERE id = 9801;

                INSERT INTO content_review_activity (
                    content_post_id, revision_id, action, actor_dashboard_user_id, created_at
                ) VALUES (9801, 9801, 'SUBMITTED_FOR_REVIEW', 9801, now());
                """);

        assertThatThrownBy(() -> execute("""
                INSERT INTO content_post_revision (
                    content_post_id, revision_number, content_type, title, slug,
                    robots_index, robots_follow, content_document,
                    content_document_schema_version, created_from_post_version,
                    created_by_dashboard_user_id, revision_reason, created_at
                ) VALUES (
                    9801, 1, 'ARTICLE', 'Duplicate', 'workflow-post', true, true,
                    '{"schemaVersion":2,"blocks":[]}'::jsonb, 2, 1,
                    9801, 'REVIEW_SUBMISSION', now()
                )
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23505");

        assertThatThrownBy(() -> execute(
                "UPDATE content_post_revision SET title = 'Mutated' WHERE id = 9801"
        ))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("55000");

        assertThatThrownBy(() -> execute(
                "UPDATE content_review_activity SET comment = 'Mutated' WHERE content_post_id = 9801"
        ))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("55000");

        assertThatThrownBy(() -> execute("""
                UPDATE content_post
                SET current_review_revision_id = 9801, status = 'IN_REVIEW'
                WHERE id = 9802
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23503");

        assertThatThrownBy(() -> execute("""
                INSERT INTO content_review_activity (
                    content_post_id, revision_id, action, actor_dashboard_user_id, created_at
                ) VALUES (9801, 9801, 'CHANGES_REQUESTED', 9802, now())
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23514");

        assertThat(text("SELECT title FROM content_post_revision WHERE id = 9801"))
                .isEqualTo("Reviewed title");
    }

    @Test
    void productionShapedV134UpgradePreservesRowsAndRepairsLegacyShapes()
            throws Exception {

        Flyway baselineFlyway = flyway(MigrationVersion.fromVersion("134"));

        assertThat(baselineFlyway.migrate().success).isTrue();

        execute("""
                INSERT INTO city (
                    id,
                    name,
                    slug,
                    state,
                    latitude,
                    longitude,
                    growth_percent
                )
                VALUES (
                    9001,
                    'Migration City',
                    'migration-city',
                    'Migration State',
                    28.6123456,
                    77.2123456,
                    7.25
                );

                INSERT INTO category (
                    id,
                    name,
                    slug,
                    priority,
                    active
                )
                VALUES (
                    9001,
                    'Migration Category',
                    'migration-category',
                    1,
                    true
                );

                INSERT INTO users (
                    id,
                    phone_number,
                    is_verified,
                    role,
                    onboarding_status
                )
                VALUES (
                    9001,
                    '+919999999991',
                    true,
                    'CUSTOMER',
                    'CUSTOMER_READY'
                );

                INSERT INTO business (
                    id,
                    name,
                    city_id,
                    category_id,
                    avg_rating,
                    latitude,
                    longitude
                )
                VALUES (
                    9001,
                    'Migration Business',
                    9001,
                    9001,
                    4.75,
                    28.61,
                    77.21
                );

                INSERT INTO company (
                    id,
                    name,
                    slug,
                    company_type
                )
                VALUES (
                    9001,
                    'Migration Company',
                    'migration-company',
                    'ARCHITECT&DESIGNERS'
                );

                INSERT INTO company_stat (
                    id,
                    company_id,
                    label,
                    value
                )
                VALUES (
                    9001,
                    9001,
                    'Preserved',
                    'Yes'
                );

                INSERT INTO builder (
                    id,
                    name
                )
                VALUES (
                    9001,
                    'Migration Builder'
                );

                INSERT INTO project (
                    id,
                    builder_id,
                    name,
                    slug,
                    review_status
                )
                VALUES (
                    9001,
                    9001,
                    'Migration Project',
                    'migration-project',
                    'DRAFT'
                );

                INSERT INTO project_property_types (
                    project_id,
                    property_type
                )
                VALUES (
                    9001,
                    'APARTMENT'
                );

                INSERT INTO project_media (
                    id,
                    project_id,
                    media_type,
                    url
                )
                VALUES (
                    9001,
                    9001,
                    'IMAGE',
                    'https://example.invalid/migration.webp'
                );

                INSERT INTO login_history (
                    id,
                    user_id,
                    login_type,
                    success
                )
                VALUES (
                    9001,
                    9001,
                    'OTP',
                    true
                );
                """);

        execute("""
                ALTER TABLE business
                    ALTER COLUMN avg_rating
                    TYPE NUMERIC(8, 4)
                    USING avg_rating::numeric;

                ALTER TABLE business
                    ALTER COLUMN latitude
                    TYPE NUMERIC(10, 7)
                    USING latitude::numeric;

                ALTER TABLE business
                    ALTER COLUMN longitude
                    TYPE NUMERIC(10, 7)
                    USING longitude::numeric;

                ALTER TABLE city
                    ALTER COLUMN latitude
                    TYPE NUMERIC(10, 7)
                    USING latitude::numeric;

                ALTER TABLE city
                    ALTER COLUMN longitude
                    TYPE NUMERIC(10, 7)
                    USING longitude::numeric;

                ALTER TABLE city
                    ALTER COLUMN growth_percent
                    TYPE NUMERIC(8, 2)
                    USING growth_percent::numeric;

                ALTER TABLE app_screen_content
                    ALTER COLUMN aspect_ratio
                    TYPE NUMERIC(8, 4)
                    USING aspect_ratio::numeric;

                ALTER TABLE provider_media
                    DROP COLUMN updated_at;

                ALTER TABLE home_section_item
                    DROP COLUMN group_key;

                ALTER TABLE distributor
                    DROP COLUMN logo_url;

                ALTER TABLE company_project
                    DROP COLUMN client_name;

                ALTER TABLE company_project
                    DROP COLUMN project_area;

                ALTER TABLE company_project
                    DROP COLUMN detail3;

                ALTER TABLE company_project
                    DROP COLUMN tags;
                """);

        Flyway flyway = flyway(null);

        assertThat(flyway.migrate().success).isTrue();
        flyway.validate();

        assertThat(text("""
                SELECT name
                FROM business
                WHERE id = 9001
                """))
                .isEqualTo("Migration Business");

        assertThat(text("""
                SELECT value
                FROM company_stat
                WHERE id = 9001
                """))
                .isEqualTo("Yes");

        assertThat(text("""
                SELECT name
                FROM project
                WHERE id = 9001
                """))
                .isEqualTo("Migration Project");

        assertThat(text("""
                SELECT property_type
                FROM project_property_types
                WHERE project_id = 9001
                """))
                .isEqualTo("APARTMENT");

        assertThat(text("""
                SELECT url
                FROM project_media
                WHERE id = 9001
                """))
                .isEqualTo("https://example.invalid/migration.webp");

        assertThat(number("""
                SELECT count(*)
                FROM login_history
                WHERE id = 9001
                  AND user_id = 9001
                """))
                .isOne();

        assertColumnType(
                "business",
                "avg_rating",
                "double precision"
        );

        assertColumnType(
                "business",
                "latitude",
                "double precision"
        );

        assertColumnType(
                "city",
                "growth_percent",
                "double precision"
        );

        assertColumnType(
                "app_screen_content",
                "aspect_ratio",
                "double precision"
        );

        assertColumn(
                "provider_media",
                "updated_at"
        );

        assertColumn(
                "home_section_item",
                "group_key"
        );

        assertColumn(
                "distributor",
                "logo_url"
        );

        assertColumn(
                "company_project",
                "client_name"
        );

        assertThat(text("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'login_history'
                  AND column_name = 'user_id'
                """))
                .isEqualTo("YES");

        assertThat(text("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """))
                .isEqualTo("156");

        assertIndex("uk_company_project_slug");
    }

    @Test
    void alreadyCorrectSchemaUpgradeIsNonDestructiveAndHasExpectedConstraintsAndIndexes()
            throws Exception {

        Flyway baselineFlyway = flyway(MigrationVersion.fromVersion("134"));

        assertThat(baselineFlyway.migrate().success).isTrue();

        execute("""
                INSERT INTO city (
                    id,
                    name,
                    slug,
                    state
                )
                VALUES (
                    9101,
                    'Correct City',
                    'correct-city',
                    'State'
                );

                INSERT INTO company (
                    id,
                    name,
                    slug,
                    company_type
                )
                VALUES (
                    9101,
                    'Correct Company',
                    'correct-company',
                    'ARCHITECT&DESIGNERS'
                );
                """);

        Flyway flyway = flyway(null);

        assertThat(flyway.migrate().success).isTrue();
        flyway.validate();

        assertThat(text("""
                SELECT name
                FROM company
                WHERE id = 9101
                """))
                .isEqualTo("Correct Company");

        assertConstraint("company", "p");
        assertConstraint("project", "p");
        assertConstraint("project_property_types", "f");
        assertConstraint("feed_section_item", "f");

        assertIndex("idx_builder_public");
        assertIndex("idx_project_city_id");
        assertIndex("idx_feed_section_item_config");
        assertIndex("uk_company_project_slug");

        assertThat(text("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """))
                .isEqualTo("156");
    }

    @Test
    void guestIdentityEpochMigrationBackfillsLegacyLinksAndEnforcesAppendOnlyHistory()
            throws Exception {

        Flyway beforeIdentityEpochs = flyway(MigrationVersion.fromVersion("147"));
        assertThat(beforeIdentityEpochs.migrate().success).isTrue();

        execute("""
                INSERT INTO users (
                    id, email, password, phone_number, is_verified, role,
                    onboarding_status, is_admin
                ) VALUES
                    (9201, 'guest-a@phone.local', 'encoded', '+919876509201', TRUE,
                     'CUSTOMER', 'ROLE_PENDING', FALSE),
                    (9202, 'guest-b@phone.local', 'encoded', '+919876509202', TRUE,
                     'CUSTOMER', 'ROLE_PENDING', FALSE);

                INSERT INTO guest_sessions (
                    id, installation_id, active, linked_user_id, linked_at,
                    created_at, updated_at, last_seen_at
                ) VALUES (
                    9201, 'legacy-shared-installation', TRUE, 9201, now(),
                    now(), now(), now()
                );
                """);

        Flyway afterIdentityEpochs = flyway(null);
        assertThat(afterIdentityEpochs.migrate().success).isTrue();
        afterIdentityEpochs.validate();

        assertThat(number("""
                SELECT count(*)
                FROM guest_identity_link
                WHERE guest_session_id = 9201
                  AND user_id = 9201
                  AND installation_id = 'legacy-shared-installation'
                  AND link_type = 'OTP_CONVERSION'
                """)).isOne();

        assertThat(text("""
                SELECT active::text
                FROM guest_sessions
                WHERE id = 9201
                """)).isEqualTo("false");

        execute("""
                INSERT INTO guest_sessions (
                    id, installation_id, active, created_at, updated_at, last_seen_at
                ) VALUES (
                    9202, 'legacy-shared-installation', TRUE, now(), now(), now()
                )
                """);

        assertThatThrownBy(() -> execute("""
                INSERT INTO guest_sessions (
                    id, installation_id, active, created_at, updated_at, last_seen_at
                ) VALUES (
                    9203, 'legacy-shared-installation', TRUE, now(), now(), now()
                )
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("23505");

        assertThatThrownBy(() -> execute("""
                UPDATE guest_sessions
                SET linked_user_id = 9202
                WHERE id = 9201
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("P0001");

        assertThatThrownBy(() -> execute("""
                UPDATE guest_identity_link
                SET user_id = 9202
                WHERE guest_session_id = 9201
                """))
                .isInstanceOf(SQLException.class)
                .extracting(exception -> ((SQLException) exception).getSQLState())
                .isEqualTo("P0001");
    }

    @Test
    void baselinePgTrgmRequiresAnExtensionOwningMigrationRole()
            throws Exception {

        execute("DROP ROLE IF EXISTS sfs_restricted_migration");

        execute("""
                CREATE ROLE sfs_restricted_migration
                LOGIN
                PASSWORD 'sfs_restricted_migration'
                """);

        execute("""
                GRANT USAGE, CREATE
                ON SCHEMA public
                TO sfs_restricted_migration
                """);

        execute("""
                GRANT CONNECT
                ON DATABASE %s
                TO sfs_restricted_migration
                """.formatted(postgres.getDatabaseName()));

        try (
                Connection restricted = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        "sfs_restricted_migration",
                        "sfs_restricted_migration"
                );
                Statement statement = restricted.createStatement()
        ) {
            assertThatThrownBy(() ->
                    statement.execute(
                            "CREATE EXTENSION pg_trgm WITH SCHEMA public"
                    )
            )
                    .isInstanceOf(SQLException.class)
                    .extracting(exception ->
                            ((SQLException) exception).getSQLState()
                    )
                    .isEqualTo("42501");
        }

        execute("CREATE EXTENSION pg_trgm WITH SCHEMA public");

        try (
                Connection restricted = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        "sfs_restricted_migration",
                        "sfs_restricted_migration"
                );
                Statement statement = restricted.createStatement()
        ) {
            assertThatThrownBy(() ->
                    statement.execute(
                            "COMMENT ON EXTENSION pg_trgm "
                                    + "IS 'restricted migration test'"
                    )
            )
                    .isInstanceOf(SQLException.class)
                    .extracting(exception ->
                            ((SQLException) exception).getSQLState()
                    )
                    .isEqualTo("42501");
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .cleanDisabled(true);

        if (target != null) {
            configuration.target(target);
        }

        return configuration.load();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    private void execute(String sql) throws SQLException {
        try (
                Connection connection = connection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(sql);
        }
    }

    private String text(String sql) throws SQLException {
        try (
                Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)
        ) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private long number(String sql) throws SQLException {
        return Long.parseLong(text(sql));
    }

    private void assertColumn(
            String table,
            String column
    ) throws SQLException {

        assertThat(number("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
                """.formatted(table, column)))
                .isOne();
    }

    private void assertColumnType(
            String table,
            String column,
            String expected
    ) throws SQLException {

        assertThat(text("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
                """.formatted(table, column)))
                .isEqualTo(expected);
    }

    private void assertConstraint(
            String table,
            String type
    ) throws SQLException {

        assertThat(number("""
                SELECT count(*)
                FROM pg_constraint
                WHERE conrelid = 'public.%s'::regclass
                  AND contype = '%s'
                """.formatted(table, type)))
                .isPositive();
    }

    private void assertIndex(String index) throws SQLException {
        assertThat(number("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = '%s'
                """.formatted(index)))
                .isOne();
    }
}
