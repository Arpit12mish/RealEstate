package com.brandPitara.sfs.marketplace.seed;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway history on an empty PostgreSQL, then the demo seed the way production meets it:
 * first with placeholder numbers (stores only), then with real numbers (workers, connections and the
 * contact number are filled in), then again (nothing changes).
 */
@Testcontainers(disabledWithoutDocker = true)
class MarketplaceDemoSeedPostgresIntegrationTest {

    static final String TAG = "https://fixtures.sfs.local/marketplace";
    static final String LOGINS = "9876500001, +91 98765 00002, 09876500003, 9876500004";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marketplace_seed")
            .withUsername("seed_test")
            .withPassword("seed_test");

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();
        ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.update("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES (9301, 'Seed Reviewer', 'seed-reviewer@example.com', 'encoded', 'ADMIN', TRUE, now(), now())
                """);
    }

    @Test
    void loadsStoresFirstThenFillsInWorkersAndContactsOnceNumbersAreConfigured() throws Exception {
        // 1) First production start with placeholders left in the env file.
        MarketplaceDemoSeedRunner.Result first =
                new MarketplaceDemoSeedRunner(ds, "879XXXXXXX", "9XXXXXXXXX,9XXXXXXXXX,9XXXXXXXXX,9XXXXXXXXX").seed();

        // 4 dealers (the V6 sample stores are not part of a fresh baseline database), no workers
        assertThat(first.inserted()).isEqualTo(4);
        assertThat(first.contactConfigured()).isFalse();
        assertThat(count("SELECT count(*) FROM business WHERE website = ? AND primary_phone IS NULL", TAG)).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM provider_profile")).isZero();

        Long main = jdbc.queryForObject("SELECT id FROM business WHERE name = 'Sharma Paints & Hardware'", Long.class);
        assertThat(count("SELECT count(*) FROM business_media WHERE business_id = ? AND usage_type = 'HERO'", main)).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM business_media WHERE business_id = ? AND usage_type = 'GALLERY'", main)).isEqualTo(10);
        assertThat(count("SELECT count(*) FROM business_offering_group WHERE business_id = ?", main)).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM business_opening_hours WHERE business_id = ?", main)).isEqualTo(7);
        assertThat(count("SELECT count(DISTINCT category_id) FROM business WHERE name IN "
                + "('Sharma Paints & Hardware', 'Elite Home Solutions', 'ProPaint Colour Studio')")).isEqualTo(1);

        // 2) Restart after the real numbers were added.
        MarketplaceDemoSeedRunner.Result second = new MarketplaceDemoSeedRunner(ds, "87900 12345", LOGINS).seed();

        assertThat(second.inserted()).isEqualTo(4);          // the 4 workers
        assertThat(second.contactsUpdated()).isEqualTo(4);   // the 4 stores created without a number
        assertThat(count("SELECT count(*) FROM business WHERE website = ?", TAG)).isEqualTo(8);
        assertThat(count("""
                SELECT count(*) FROM business
                WHERE website = ? AND primary_phone = '+918790012345' AND whatsapp_phone = '+918790012345'
                """, TAG)).isEqualTo(8);
        assertThat(count("""
                SELECT count(*) FROM provider_profile p JOIN users u ON u.id = p.user_id
                WHERE p.provider_type = 'WORKER' AND p.verification_status = 'VERIFIED'
                  AND u.phone_number IN ('+919876500001', '+919876500002', '+919876500003', '+919876500004')
                """)).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM business_worker_link")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM business_worker_link WHERE recommendation_status = 'VERIFIED'")).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM provider_rate")).isEqualTo(7);
        assertThat(count("SELECT count(*) FROM business_review")).isZero();
        assertThat(count("SELECT count(*) FROM business_media")).isEqualTo(26);

        // 3) Any later restart changes nothing - and a link removed in the dashboard stays removed.
        jdbc.update("DELETE FROM business_worker_link WHERE recommendation_status = 'NONE'");
        MarketplaceDemoSeedRunner.Result third = new MarketplaceDemoSeedRunner(ds, "87900 12345", LOGINS).seed();

        assertThat(third.inserted()).isZero();
        assertThat(third.contactsUpdated()).isZero();
        assertThat(count("SELECT count(*) FROM business WHERE website = ?", TAG)).isEqualTo(8);
        assertThat(count("SELECT count(*) FROM business_media")).isEqualTo(26);
        assertThat(count("SELECT count(*) FROM business_worker_link")).isEqualTo(3);
    }

    private static int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
