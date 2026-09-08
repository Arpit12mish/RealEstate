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
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_forward_migration")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @BeforeEach
    void resetSchema() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
        }
    }

    @Test
    void freshPostgresUsesBaselineThenForwardRepairWithoutCallback() throws Exception {
        Flyway flyway = flyway(null);

        assertThat(flyway.migrate().success).isTrue();
        flyway.validate();

        assertThat(text("SELECT version FROM flyway_schema_history WHERE type = 'SQL_BASELINE'"))
                .isEqualTo("134");
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version = '141' AND success"))
                .isOne();
        assertThat(number("SELECT count(*) FROM flyway_schema_history "
                + "WHERE version::integer BETWEEN 135 AND 139"))
                .isZero();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE NOT success")).isZero();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE lower(script) LIKE '%callback%'"))
                .isZero();
        assertThat(number("SELECT count(*) FROM flyway_schema_history WHERE version = '140'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM category")).isEqualTo(68);
        assertThat(number("SELECT count(*) FROM city")).isEqualTo(11);
        assertThat(number("SELECT count(*) FROM content_version")).isEqualTo(5);
        assertThat(number("SELECT count(*) FROM home_section_config WHERE home_category_id = 0"))
                .isEqualTo(7);
        assertThat(number("SELECT count(*) FROM promo_banner_slot_config "
                + "WHERE screen='HOME' AND home_category_id=0 AND slot_key='HERO'"))
                .isOne();
        assertThat(number("SELECT count(*) FROM business")).isZero();
        assertThat(number("SELECT count(*) FROM brand")).isZero();
        assertThat(number("SELECT count(*) FROM company")).isZero();
        assertThat(number("SELECT count(*) FROM promo_banner")).isZero();
        assertThat(text("SELECT parent.slug FROM category child JOIN category parent "
                + "ON parent.id=child.parent_id WHERE child.slug='plumbers'"))
                .isEqualTo("home-repair-maintenance");
        assertThat(text("SELECT parent.slug FROM category child JOIN category parent "
                + "ON parent.id=child.parent_id WHERE child.slug='tiles-flooring'"))
                .isEqualTo("flooring-tiles");
    }

    @Test
    void productionShapedV134UpgradePreservesRowsAndRepairsLegacyShapes() throws Exception {
        flyway(MigrationVersion.fromVersion("134")).migrate();

        execute("""
                INSERT INTO city (id, name, slug, state, latitude, longitude, growth_percent)
                VALUES (9001, 'Migration City', 'migration-city', 'Migration State', 28.6123456, 77.2123456, 7.25);
                INSERT INTO category (id, name, slug, priority, active)
                VALUES (9001, 'Migration Category', 'migration-category', 1, true);
                INSERT INTO users (id, phone_number, is_verified, role, onboarding_status)
                VALUES (9001, '+919999999991', true, 'CUSTOMER', 'CUSTOMER_READY');
                INSERT INTO business (id, name, city_id, category_id, avg_rating, latitude, longitude)
                VALUES (9001, 'Migration Business', 9001, 9001, 4.75, 28.61, 77.21);
                INSERT INTO company (id, name, slug, company_type)
                VALUES (9001, 'Migration Company', 'migration-company', 'ARCHITECT&DESIGNERS');
                INSERT INTO company_stat (id, company_id, label, value)
                VALUES (9001, 9001, 'Preserved', 'Yes');
                INSERT INTO builder (id, name) VALUES (9001, 'Migration Builder');
                INSERT INTO project (id, builder_id, name, slug, review_status)
                VALUES (9001, 9001, 'Migration Project', 'migration-project', 'DRAFT');
                INSERT INTO project_property_types (project_id, property_type)
                VALUES (9001, 'APARTMENT');
                INSERT INTO project_media (id, project_id, media_type, url)
                VALUES (9001, 9001, 'IMAGE', 'https://example.invalid/migration.webp');
                INSERT INTO login_history (id, user_id, login_type, success)
                VALUES (9001, 9001, 'OTP', true);
                """);

        execute("""
                ALTER TABLE business ALTER COLUMN avg_rating TYPE NUMERIC(8,4) USING avg_rating::numeric;
                ALTER TABLE business ALTER COLUMN latitude TYPE NUMERIC(10,7) USING latitude::numeric;
                ALTER TABLE business ALTER COLUMN longitude TYPE NUMERIC(10,7) USING longitude::numeric;
                ALTER TABLE city ALTER COLUMN latitude TYPE NUMERIC(10,7) USING latitude::numeric;
                ALTER TABLE city ALTER COLUMN longitude TYPE NUMERIC(10,7) USING longitude::numeric;
                ALTER TABLE city ALTER COLUMN growth_percent TYPE NUMERIC(8,2) USING growth_percent::numeric;
                ALTER TABLE app_screen_content ALTER COLUMN aspect_ratio TYPE NUMERIC(8,4)
                    USING aspect_ratio::numeric;
                ALTER TABLE provider_media DROP COLUMN updated_at;
                ALTER TABLE home_section_item DROP COLUMN group_key;
                ALTER TABLE distributor DROP COLUMN logo_url;
                ALTER TABLE company_project DROP COLUMN client_name;
                ALTER TABLE company_project DROP COLUMN project_area;
                ALTER TABLE company_project DROP COLUMN detail3;
                ALTER TABLE company_project DROP COLUMN tags;
                """);

        Flyway flyway = flyway(null);
        assertThat(flyway.migrate().success).isTrue();
        flyway.validate();

        assertThat(text("SELECT name FROM business WHERE id = 9001")).isEqualTo("Migration Business");
        assertThat(text("SELECT value FROM company_stat WHERE id = 9001")).isEqualTo("Yes");
        assertThat(text("SELECT name FROM project WHERE id = 9001")).isEqualTo("Migration Project");
        assertThat(text("SELECT property_type FROM project_property_types WHERE project_id = 9001"))
                .isEqualTo("APARTMENT");
        assertThat(text("SELECT url FROM project_media WHERE id = 9001"))
                .isEqualTo("https://example.invalid/migration.webp");
        assertThat(number("SELECT count(*) FROM login_history WHERE id = 9001 AND user_id = 9001"))
                .isOne();

        assertColumnType("business", "avg_rating", "double precision");
        assertColumnType("business", "latitude", "double precision");
        assertColumnType("city", "growth_percent", "double precision");
        assertColumnType("app_screen_content", "aspect_ratio", "double precision");
        assertColumn("provider_media", "updated_at");
        assertColumn("home_section_item", "group_key");
        assertColumn("distributor", "logo_url");
        assertColumn("company_project", "client_name");
        assertThat(text("SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_schema='public' AND table_name='login_history' AND column_name='user_id'"))
                .isEqualTo("YES");
    }

    @Test
    void alreadyCorrectSchemaUpgradeIsNonDestructiveAndHasExpectedConstraintsAndIndexes() throws Exception {
        flyway(MigrationVersion.fromVersion("134")).migrate();
        execute("""
                INSERT INTO city (id, name, slug, state) VALUES (9101, 'Correct City', 'correct-city', 'State');
                INSERT INTO company (id, name, slug, company_type)
                VALUES (9101, 'Correct Company', 'correct-company', 'ARCHITECT&DESIGNERS');
                """);

        Flyway flyway = flyway(null);
        assertThat(flyway.migrate().success).isTrue();
        assertThat(text("SELECT name FROM company WHERE id = 9101")).isEqualTo("Correct Company");

        assertConstraint("company", "p");
        assertConstraint("project", "p");
        assertConstraint("project_property_types", "f");
        assertConstraint("feed_section_item", "f");
        assertIndex("idx_builder_public");
        assertIndex("idx_project_city_id");
        assertIndex("idx_feed_section_item_config");
    }

    @Test
    void baselinePgTrgmRequiresAnExtensionOwningMigrationRole() throws Exception {
        execute("DROP ROLE IF EXISTS sfs_restricted_migration");
        execute("CREATE ROLE sfs_restricted_migration LOGIN PASSWORD 'sfs_restricted_migration'");
        execute("GRANT USAGE, CREATE ON SCHEMA public TO sfs_restricted_migration");
        execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName()
                + " TO sfs_restricted_migration");

        try (Connection restricted = DriverManager.getConnection(
                postgres.getJdbcUrl(), "sfs_restricted_migration", "sfs_restricted_migration");
             Statement statement = restricted.createStatement()) {
            assertThatThrownBy(() -> statement.execute("CREATE EXTENSION pg_trgm WITH SCHEMA public"))
                    .isInstanceOf(SQLException.class)
                    .extracting(exception -> ((SQLException) exception).getSQLState())
                    .isEqualTo("42501");
        }

        execute("CREATE EXTENSION pg_trgm WITH SCHEMA public");
        try (Connection restricted = DriverManager.getConnection(
                postgres.getJdbcUrl(), "sfs_restricted_migration", "sfs_restricted_migration");
             Statement statement = restricted.createStatement()) {
            assertThatThrownBy(() -> statement.execute(
                    "COMMENT ON EXTENSION pg_trgm IS 'restricted migration test'"))
                    .isInstanceOf(SQLException.class)
                    .extracting(exception -> ((SQLException) exception).getSQLState())
                    .isEqualTo("42501");
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .cleanDisabled(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String text(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private long number(String sql) throws SQLException {
        return Long.parseLong(text(sql));
    }

    private void assertColumn(String table, String column) throws SQLException {
        assertThat(number("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' "
                + "AND table_name='" + table + "' AND column_name='" + column + "'"))
                .isOne();
    }

    private void assertColumnType(String table, String column, String expected) throws SQLException {
        assertThat(text("SELECT data_type FROM information_schema.columns WHERE table_schema='public' "
                + "AND table_name='" + table + "' AND column_name='" + column + "'"))
                .isEqualTo(expected);
    }

    private void assertConstraint(String table, String type) throws SQLException {
        assertThat(number("SELECT count(*) FROM pg_constraint WHERE conrelid='public."
                + table + "'::regclass AND contype='" + type + "'"))
                .isPositive();
    }

    private void assertIndex(String index) throws SQLException {
        assertThat(number("SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname='"
                + index + "'"))
                .isOne();
    }
}
