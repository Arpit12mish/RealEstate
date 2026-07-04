package com.brandPitara.sfs.migration;

import org.junit.jupiter.api.BeforeEach;
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
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the actual V110 migration SQL (read from the classpath, not copy-pasted) against
 * a minimal `users` table to prove:
 *  1) it aborts safely with a clear error instead of a cryptic constraint-violation
 *     error when existing data isn't already canonical, and
 *  2) once applied, the CHECK constraint actually rejects non-canonical phone numbers
 *     at the database level (not just via application code).
 */
@Testcontainers(disabledWithoutDocker = true)
class PhoneNumberCheckConstraintMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_migration_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    private static final String CREATE_MINIMAL_USERS_TABLE = """
            CREATE TABLE users (
                id BIGSERIAL PRIMARY KEY,
                phone_number VARCHAR(20)
            )
            """;

    private String migrationSql;

    @BeforeEach
    void loadMigration() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V110__enforce_canonical_phone_number_format.sql");
        migrationSql = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    @Test
    void abortsWithClearErrorWhenExistingRowsAreNotCanonical() throws Exception {
        try (Connection connection = openConnection()) {
            execute(connection, CREATE_MINIMAL_USERS_TABLE);
            execute(connection, "INSERT INTO users (phone_number) VALUES ('9876543210')"); // legacy, non-canonical

            assertThatThrownBy(() -> execute(connection, migrationSql))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("V110 aborted");
        }
    }

    @Test
    void appliesSuccessfullyAndRejectsNonCanonicalInsertsAfterward() throws Exception {
        try (Connection connection = openConnection()) {
            execute(connection, CREATE_MINIMAL_USERS_TABLE);
            execute(connection, "INSERT INTO users (phone_number) VALUES ('+919876543210')"); // already canonical

            execute(connection, migrationSql);

            // Canonical phone numbers keep working.
            execute(connection, "INSERT INTO users (phone_number) VALUES ('+916876543211')");

            // A non-canonical value is now rejected at the DB level, not just by app code.
            assertThatThrownBy(() -> execute(connection, "INSERT INTO users (phone_number) VALUES ('9876543212')"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ck_users_phone_number_canonical_e164");
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
