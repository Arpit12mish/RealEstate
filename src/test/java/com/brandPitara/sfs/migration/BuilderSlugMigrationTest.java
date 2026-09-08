package com.brandPitara.sfs.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the actual V136 migration SQL (read from the classpath, not
 * copy-pasted) against a minimal reconstructed `builder` table, proving the
 * real backfill/collision/constraint behavior described in
 * V136__add_builder_slug.sql - the same "run the real file" discipline as
 * PhoneNumberCheckConstraintMigrationTest (V110), but each test method here
 * drops and recreates its own `builder` table first so the shared static
 * container never leaks state between methods (the exact bug that makes
 * PhoneNumberCheckConstraintMigrationTest's second method fail with
 * "relation users already exists" - deliberately not repeated here).
 */
@Testcontainers(disabledWithoutDocker = true)
class BuilderSlugMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_migration_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    private static final String CREATE_MINIMAL_BUILDER_TABLE = """
            CREATE TABLE builder (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(150) NOT NULL,
                active BOOLEAN NOT NULL DEFAULT true,
                published BOOLEAN NOT NULL DEFAULT false,
                deleted BOOLEAN NOT NULL DEFAULT false
            )
            """;

    private String migrationSql() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V136__add_builder_slug.sql");
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void freshBuilderTable(Connection connection) throws SQLException {
        execute(connection, "DROP TABLE IF EXISTS builder CASCADE");
        execute(connection, CREATE_MINIMAL_BUILDER_TABLE);
    }

    private List<String[]> slugsOrderedById(Connection connection) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, slug FROM builder ORDER BY id")) {
            while (rs.next()) {
                rows.add(new String[] { rs.getString("id"), rs.getString("slug") });
            }
        }
        return rows;
    }

    @Test
    void emptyBuilderTable_migratesCleanlyWithNoRows() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, migrationSql());
            assertThat(slugsOrderedById(connection)).isEmpty();
        }
    }

    @Test
    void oneBuilder_getsSlugifiedNameAsSlug() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('Meridian Constructions')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[1]).isEqualTo("meridian-constructions");
        }
    }

    @Test
    void multipleUniqueBuilders_eachGetsItsOwnSlug_noCollisionSuffix() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('Meridian Constructions')");
            execute(connection, "INSERT INTO builder (name) VALUES ('Harborview Developers')");
            execute(connection, "INSERT INTO builder (name) VALUES ('Silverline Realty Builders')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).extracting(r -> r[1]).containsExactly(
                    "meridian-constructions", "harborview-developers", "silverline-realty-builders");
        }
    }

    @Test
    void duplicateNames_getDeterministicNumericSuffixesByIdOrder() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).extracting(r -> r[1]).containsExactly(
                    "m3m-india", "m3m-india-2", "m3m-india-3");
        }
    }

    @Test
    void punctuationAndCaseEquivalentNames_collapseToSameBaseSlug_thenGetSuffixed() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            // "M3M India", "M3M-India", "m3m  india" all normalize to the same base_slug.
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M-India')");
            execute(connection, "INSERT INTO builder (name) VALUES ('m3m  india')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).extracting(r -> r[1]).containsExactly(
                    "m3m-india", "m3m-india-2", "m3m-india-3");
            // Every slug is unique, regardless of the source-name variation that produced it.
            assertThat(new HashSet<>(rows.stream().map(r -> r[1]).toList())).hasSize(3);
        }
    }

    @Test
    void unicodeNames_normalizeSafelyWithoutFailingTheMigration() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('Café Constructions Île')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).hasSize(1);
            // Non-ASCII characters collapse to separators (same policy as brand.slug) -
            // the important guarantee is a non-blank, unique, lowercase-ASCII-kebab slug,
            // not a true transliteration.
            String slug = rows.get(0)[1];
            assertThat(slug).isNotBlank();
            assertThat(slug).matches("[a-z0-9-]+");
            assertThat(slug).doesNotStartWith("-").doesNotEndWith("-");
        }
    }

    @Test
    void deletedBuilders_stillReceiveAndKeepAUniqueSlug() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name, deleted) VALUES ('Retired Builder Co', true)");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[1]).isEqualTo("retired-builder-co");
        }
    }

    @Test
    void inactiveBuilders_stillReceiveAUniqueSlug() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name, active) VALUES ('Paused Builder Co', false)");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[1]).isEqualTo("paused-builder-co");
        }
    }

    @Test
    void deletedBuilderSlugIsNotReusableByANewActiveBuilderAfterMigration() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name, deleted) VALUES ('Retired Builder Co', true)");
            execute(connection, migrationSql());

            // The unique constraint is table-wide, not scoped to deleted = false -
            // a new builder with the exact same slug value must be rejected.
            assertThatThrownBy(() ->
                    execute(connection, "INSERT INTO builder (name, slug) VALUES ('New Co', 'retired-builder-co')"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_builder_slug");
        }
    }

    @Test
    void emptyOrWhitespaceOnlyName_fallsBackToBuilderIdSafetyNet() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('   ')");
            execute(connection, migrationSql());

            List<String[]> rows = slugsOrderedById(connection);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[1]).isEqualTo("builder-" + rows.get(0)[0]);
        }
    }

    @Test
    void afterMigration_slugColumnIsNotNullAndUniqueAtTheDatabaseLevel() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('Meridian Constructions')");
            execute(connection, migrationSql());

            assertThatThrownBy(() -> execute(connection, "UPDATE builder SET slug = NULL"))
                    .isInstanceOf(SQLException.class);

            execute(connection, "INSERT INTO builder (name, slug) VALUES ('Another Co', 'another-co')");
            assertThatThrownBy(() ->
                    execute(connection, "INSERT INTO builder (name, slug) VALUES ('Yet Another Co', 'another-co')"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_builder_slug");
        }
    }

    @Test
    void migrationIsIdempotent_reRunningAfterSuccessMakesNoChanges() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('Meridian Constructions')");
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, "INSERT INTO builder (name) VALUES ('M3M India')");
            execute(connection, migrationSql());

            List<List<String>> before = slugsOrderedById(connection).stream().map(List::of).toList();

            // Re-running conceptually (the ALTER TABLE ADD COLUMN IF NOT EXISTS / backfill
            // WHERE slug IS NULL / DO-block constraint guard are all written to be safe to
            // re-run) must not change any existing slug or fail outright.
            execute(connection, migrationSql());

            List<List<String>> after = slugsOrderedById(connection).stream().map(List::of).toList();
            assertThat(after).isEqualTo(before);
        }
    }

    @Test
    void backfilledSlugsAreLowercaseAsciiKebabCaseWithNoLeadingOrTrailingHyphen() throws Exception {
        try (Connection connection = openConnection()) {
            freshBuilderTable(connection);
            execute(connection, "INSERT INTO builder (name) VALUES ('  -- Weird///Name!! --  ')");
            execute(connection, migrationSql());

            Set<String> slugs = new HashSet<>();
            for (String[] row : slugsOrderedById(connection)) {
                slugs.add(row[1]);
            }
            for (String slug : slugs) {
                assertThat(slug).matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");
            }
        }
    }
}
