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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the actual V137 migration SQL (read from the classpath, not
 * copy-pasted) against a minimal reconstructed `company` table - the same
 * "run the real file" discipline as BuilderSlugMigrationTest (V136) and
 * PhoneNumberCheckConstraintMigrationTest (V110).
 *
 * Amended Phase 7B-GA (migration safety audit): the original version of
 * this test class (and the migration it exercises) treated a pre-existing
 * company's CURRENT `published` value as proof of whether it had ever been
 * published - unsafe, since this codebase has no audit/history table wired
 * to Company publish/unpublish transitions (confirmed: `DashboardCompanyServiceImpl`
 * never writes to `DashboardActionAuditEntity`). The migration was amended
 * to conservatively backfill ever_published = true for EVERY pre-existing
 * row regardless of its current published/active/deleted state; this test
 * class was amended to match, and extended with the canonicality-
 * verification and duplicate-detection tests the amended migration also
 * added.
 *
 * Per Phase 7B-G's own Migration Safety instruction: GAP-035 (an unrelated
 * business/city seed-ordering break at V6) already prevents a complete
 * fresh Flyway replay of this worktree's full migration history from ever
 * reaching V137 - so this test does NOT attempt a full bootstrap. It tests
 * the migration file in isolation against the minimum prerequisite Company
 * schema (the columns the migration itself reads/writes: id, slug,
 * published - plus active/deleted, added this phase purely so the
 * "deleted/inactive row still gets backfilled" scenarios can be exercised
 * directly, even though the migration's own SQL never references either
 * column), which is exactly what a real environment's already-existing
 * `company` table already has - full clean-bootstrap verification remains
 * separately blocked by GAP-035, not claimed as passing here.
 *
 * Each test method drops and recreates its own `company` table first so the
 * shared static container never leaks state between methods.
 */
@Testcontainers(disabledWithoutDocker = true)
class CompanySlugConstraintsMigrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("sfs_migration_test")
      .withUsername("sfs_test")
      .withPassword("sfs_test");

  private static final String CREATE_MINIMAL_COMPANY_TABLE = """
      CREATE TABLE company (
          id BIGSERIAL PRIMARY KEY,
          name VARCHAR(150) NOT NULL,
          slug VARCHAR(180),
          published BOOLEAN NOT NULL DEFAULT true,
          active BOOLEAN NOT NULL DEFAULT true,
          deleted BOOLEAN NOT NULL DEFAULT false
      )
      """;

  private String migrationSql() throws IOException {
    ClassPathResource resource = new ClassPathResource(
        "db/migration/V137__add_company_ever_published_and_reassert_slug_constraints.sql");
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

  private void freshCompanyTable(Connection connection) throws SQLException {
    execute(connection, "DROP TABLE IF EXISTS company CASCADE");
    execute(connection, CREATE_MINIMAL_COMPANY_TABLE);
  }

  private Map<Long, Boolean> everPublishedById(Connection connection) throws SQLException {
    Map<Long, Boolean> result = new HashMap<>();
    try (Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT id, ever_published FROM company ORDER BY id")) {
      while (rs.next()) {
        result.put(rs.getLong("id"), rs.getBoolean("ever_published"));
      }
    }
    return result;
  }

  private String slugById(Connection connection, long id) throws SQLException {
    try (Statement statement = connection.createStatement();
         ResultSet rs = statement.executeQuery("SELECT slug FROM company WHERE id = " + id)) {
      rs.next();
      return rs.getString("slug");
    }
  }

  @Test
  void emptyCompanyTable_migratesCleanlyWithNoRows() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, migrationSql());
      assertThat(everPublishedById(connection)).isEmpty();
    }
  }

  @Test
  void addsTheEverPublishedColumn() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Live Co', 'live-co', true)");
      execute(connection, migrationSql());

      assertThat(everPublishedById(connection)).containsKey(1L);
    }
  }

  // ---------- ever_published backfill: conservative, history-blind policy ----------

  @Test
  void backfillsEverPublishedTrueForACurrentlyPublishedCompany() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Live Co', 'live-co', true)");
      execute(connection, migrationSql());

      assertThat(everPublishedById(connection).values()).containsExactly(true);
    }
  }

  @Test
  void backfillsEverPublishedTrueForACurrentlyUnpublishedCompanyWithUnknownHistory() throws Exception {
    // This is the exact case the original migration got wrong: a row that is unpublished
    // TODAY may have been published in the past (no audit/history table exists to check) -
    // current published=false is not proof it was never public, so it must still be
    // conservatively backfilled to ever_published=true, exactly like a currently-published row.
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Unknown History Co', 'unknown-history-co', false)");
      execute(connection, migrationSql());

      assertThat(everPublishedById(connection).values()).containsExactly(true);
    }
  }

  @Test
  void backfillsEverPublishedTrueForAnExistingDeletedCompany() throws Exception {
    // A soft-deleted company may well have been public before it was deleted - deleted state
    // provides no evidence either way, so it gets the same conservative backfill.
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published, active, deleted) " +
          "VALUES ('Deleted Co', 'deleted-co', false, false, true)");
      execute(connection, migrationSql());

      assertThat(everPublishedById(connection).values()).containsExactly(true);
    }
  }

  @Test
  void backfillsEverPublishedTrueForAnExistingInactiveCompany() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published, active) " +
          "VALUES ('Inactive Co', 'inactive-co', false, false)");
      execute(connection, migrationSql());

      assertThat(everPublishedById(connection).values()).containsExactly(true);
    }
  }

  @Test
  void everyPreExistingRowIsBackfilledUniformlyRegardlessOfPublishedActiveOrDeletedState() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published, active, deleted) " +
          "VALUES ('Live Co', 'live-co', true, true, false)");
      execute(connection, "INSERT INTO company (name, slug, published, active, deleted) " +
          "VALUES ('Draft Co', 'draft-co', false, true, false)");
      execute(connection, "INSERT INTO company (name, slug, published, active, deleted) " +
          "VALUES ('Inactive Co', 'inactive-co', false, false, false)");
      execute(connection, "INSERT INTO company (name, slug, published, active, deleted) " +
          "VALUES ('Deleted Co', 'deleted-co', false, false, true)");
      execute(connection, migrationSql());

      Map<Long, Boolean> result = everPublishedById(connection);
      assertThat(result.values()).allSatisfy(v -> assertThat(v).isTrue());
      assertThat(result).hasSize(4);
    }
  }

  @Test
  void migrationIsIdempotent_reRunningAfterSuccessMakesNoChangesAndDoesNotFail() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Live Co', 'live-co', true)");
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Draft Co', 'draft-co', false)");
      execute(connection, migrationSql());

      Map<Long, Boolean> before = everPublishedById(connection);

      execute(connection, migrationSql());

      Map<Long, Boolean> after = everPublishedById(connection);
      assertThat(after).isEqualTo(before);
    }
  }

  // ---------- slug value preservation (no rewriting) ----------

  @Test
  void aValidExistingSlugValueIsPreservedExactlyByTheMigration() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a-co-123', true)");
      execute(connection, migrationSql());

      assertThat(slugById(connection, 1)).isEqualTo("a-co-123");
    }
  }

  @Test
  void afterMigration_slugColumnRejectsANewNullValue() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a-co', true)");
      execute(connection, migrationSql());

      assertThatThrownBy(() -> execute(connection, "UPDATE company SET slug = NULL"))
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  void afterMigration_slugColumnRejectsADuplicateValue() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a-co', true)");
      execute(connection, migrationSql());

      assertThatThrownBy(() ->
          execute(connection, "INSERT INTO company (name, slug, published) VALUES ('B Co', 'a-co', true)"))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("uk_company_slug");
    }
  }

  // ---------- defensive failure behavior on pre-existing incompatible data ----------
  // None of these tests ever assert a slug value changed - only that the migration
  // fails loudly, with an actionable, row-naming message, and touches nothing.

  @Test
  void preExistingNullSlug_failsLoudlyWithAnActionableMessageNamingTheRow() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('No Slug Co', NULL, false)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("NULL slug")
          .hasMessageContaining("ids [1]");
    }
  }

  @Test
  void preExistingUppercaseSlug_failsLoudlyNamingTheRow() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'Not-Canonical', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug")
          .hasMessageContaining("ids [1]");
    }
  }

  @Test
  void preExistingBlankSlug_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', '', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingSlugWithWhitespace_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a co', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingSlugWithUnderscore_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a_co', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingSlugWithRepeatedHyphen_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a--co', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingSlugWithLeadingHyphen_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', '-a-co', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingSlugWithTrailingHyphen_failsLoudly() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('A Co', 'a-co-', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }

  @Test
  void preExistingDuplicateExactSlugValues_failLoudlyNamingBothIds() throws Exception {
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('First Co', 'dup-slug', true)");
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Second Co', 'dup-slug', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("duplicate")
          .hasMessageContaining("dup-slug")
          .hasMessageContaining("1")
          .hasMessageContaining("2");
    }
  }

  @Test
  void preExistingCaseVariantDuplicates_areCaughtByTheCanonicalityCheckBeforeTheDuplicateCheck() throws Exception {
    // "My-Co" and "my-co" would collide under a lowercase-normalized comparison, but the
    // canonicality check runs first and rejects "My-Co" outright for being uppercase - so
    // this fails as a non-canonical-slug error, not a duplicate error. By the time the
    // duplicate check would run, every surviving slug is already guaranteed lowercase, so
    // exact-match duplicate detection there is equivalent to normalized duplicate detection.
    try (Connection connection = openConnection()) {
      freshCompanyTable(connection);
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('First Co', 'My-Co', true)");
      execute(connection, "INSERT INTO company (name, slug, published) VALUES ('Second Co', 'my-co', true)");

      assertThatThrownBy(() -> execute(connection, migrationSql()))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("non-canonical slug");
    }
  }
}
