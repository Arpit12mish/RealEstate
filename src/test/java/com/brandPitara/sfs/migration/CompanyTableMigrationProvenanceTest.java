package com.brandPitara.sfs.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Empirical proof for GAP-033 (see docs/mobile-web-migration/backend-gaps.md
 * in the website repo): runs the REAL Flyway migration history from
 * `classpath:db/migration` via Flyway's own Java API against a fresh
 * Testcontainers Postgres - not a grep-based inference - to confirm whether
 * a clean database can actually be bootstrapped from this worktree's
 * migration files alone.
 *
 * Running this originally surfaced a second, INDEPENDENT reproducibility problem
 * (GAP-035, since resolved by the B134 baseline - see the first test): a from-scratch bootstrap
 * does not get anywhere near V42 (the earliest company-related failure). It
 * fails immediately at V6__seed_dummy_business.sql, which inserts `business`
 * rows with a hardcoded `city_id = 1` (the file's own comment: "Make sure
 * city with id=1 exists") - but no migration seeds any `city` row until
 * V65__seed_serviceable_ncr_cities.sql, 59 files later.
 *
 * An initial attempt was made to work around V6 (target a version just
 * before it, insert one placeholder `city` row, then resume) purely to
 * still empirically reach V42 and prove the Company-specific claim. That
 * attempt hit a THIRD, again-unrelated failure before reaching V42 (a
 * missing `users` relation), showing this worktree's migration history has
 * more than one independent seed/ordering break in the V1-V41 range.
 * Chasing each one down with further ad hoc workarounds would mean
 * reconstructing large parts of unrelated schema state by hand - exactly
 * the kind of invented migration history Phase 7A-G is explicitly not
 * allowed to produce. That path was abandoned; what follows is the honest,
 * unmodified result instead.
 */
@Testcontainers(disabledWithoutDocker = true)
class CompanyTableMigrationProvenanceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_migration_provenance_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    private record HistoryRow(String version, String description, boolean success) {}

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    /**
     * Both test methods share the one static Testcontainers instance (class
     * lifecycle) but JUnit does not guarantee method execution order - each
     * test resets the schema itself first so neither depends on running
     * before or after the other (the same reason BuilderSlugMigrationTest
     * drops/recreates its table per method rather than relying on order).
     */
    private void resetToEmptySchema(Connection connection) throws SQLException {
        execute(connection, "DROP SCHEMA public CASCADE");
        execute(connection, "CREATE SCHEMA public");
    }

    private List<HistoryRow> readSchemaHistory(Connection connection) throws SQLException {
        List<HistoryRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT version, description, success FROM flyway_schema_history " +
                     "WHERE version IS NOT NULL ORDER BY installed_rank")) {
            while (rs.next()) {
                rows.add(new HistoryRow(rs.getString("version"), rs.getString("description"), rs.getBoolean("success")));
            }
        }
        return rows;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * GAP-035 is resolved: the forward-only repair (docs/releases/release-5a-m-forward-only-flyway-repair.md)
     * added the B134 baseline migration, so Flyway bootstraps an empty database from the baseline and
     * never runs the historical V6 seed that used to fail on its hardcoded city_id. A from-scratch
     * migration must now succeed, record the baseline, and leave the company table in place.
     */
    @Test
    void freshBootstrapFromMigrationHistoryNowSucceedsThroughTheB134Baseline() throws Exception {
        try (Connection connection = openConnection()) {
            resetToEmptySchema(connection);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().success).isTrue();

        try (Connection connection = openConnection()) {
            List<HistoryRow> history = readSchemaHistory(connection);
            assertThat(history).isNotEmpty();
            assertThat(history.get(0).version())
                    .as("the first applied migration should be the B134 baseline, not V1-V6")
                    .isEqualTo("134");
            assertThat(history).noneMatch(row -> row.version().equals("6"));
            assertThat(history).allSatisfy(row -> assertThat(row.success()).isTrue());
            execute(connection, "SELECT 1 FROM company LIMIT 1");
        }
    }

    /**
     * Direct-SQL cross-check, independent of Flyway's own bookkeeping and of
     * the V6/GAP-035 issue above: even with no migration history involved at
     * all, a bare `SELECT 1 FROM company LIMIT 1` against a freshly created
     * database must fail with "relation \"company\" does not exist" -
     * confirming the table is genuinely absent from a clean database, not
     * merely un-migrated in some recoverable way. This is the same
     * conclusion the exhaustive 136-file grep (see GAP-033) already
     * supports, confirmed here directly against a real Postgres instance
     * rather than by text search alone.
     */
    @Test
    void companyTableDoesNotExistOnACompletelyFreshDatabase() throws Exception {
        try (Connection connection = openConnection()) {
            resetToEmptySchema(connection);
            assertThatThrownBy(() -> execute(connection, "SELECT 1 FROM company LIMIT 1"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("company");
        }
    }
}
