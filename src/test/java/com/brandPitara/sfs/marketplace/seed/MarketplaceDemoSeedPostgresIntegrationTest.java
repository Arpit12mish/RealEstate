package com.brandPitara.sfs.marketplace.seed;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway history on an empty PostgreSQL, then the demo seed twice: the first run loads
 * every section of the dealer/worker screens, the second run must change nothing.
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

    @Test
    void loadsTheDemoMarketplaceOnceAndSkipsLaterRuns() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();
        DriverManagerDataSource ds = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.update("""
                INSERT INTO dashboard_users (id, name, email, password_hash, role, active, created_at, updated_at)
                VALUES (9301, 'Seed Reviewer', 'seed-reviewer@example.com', 'encoded', 'ADMIN', TRUE, now(), now())
                """);

        MarketplaceDemoSeedRunner.Result first = new MarketplaceDemoSeedRunner(ds, "87900 12345", LOGINS).seed();

        // 4 dealers + 4 workers (the V6 sample stores are not part of a fresh baseline database)
        assertThat(first.inserted()).isEqualTo(8);
        assertThat(first.contactConfigured()).isTrue();
        assertThat(count(jdbc, "SELECT count(*) FROM business WHERE website = ?", TAG)).isEqualTo(8);
        assertThat(count(jdbc, """
                SELECT count(*) FROM business
                WHERE website = ? AND primary_phone = '+918790012345' AND whatsapp_phone = '+918790012345'
                """, TAG)).isEqualTo(8);

        Long main = jdbc.queryForObject("SELECT id FROM business WHERE name = 'Sharma Paints & Hardware'", Long.class);
        assertThat(count(jdbc, "SELECT count(*) FROM business_media WHERE business_id = ? AND usage_type = 'HERO'", main)).isEqualTo(4);
        assertThat(count(jdbc, "SELECT count(*) FROM business_media WHERE business_id = ? AND usage_type = 'GALLERY'", main)).isEqualTo(10);
        assertThat(count(jdbc, "SELECT count(*) FROM business_offering_group WHERE business_id = ?", main)).isEqualTo(4);
        assertThat(count(jdbc, "SELECT count(*) FROM business_opening_hours WHERE business_id = ?", main)).isEqualTo(7);
        assertThat(count(jdbc, "SELECT count(DISTINCT category_id) FROM business WHERE name IN "
                + "('Sharma Paints & Hardware', 'Elite Home Solutions', 'ProPaint Colour Studio')")).isEqualTo(1);

        assertThat(count(jdbc, """
                SELECT count(*) FROM provider_profile p JOIN users u ON u.id = p.user_id
                WHERE p.provider_type = 'WORKER' AND p.verification_status = 'VERIFIED'
                  AND u.phone_number IN ('+919876500001', '+919876500002', '+919876500003', '+919876500004')
                """)).isEqualTo(4);
        assertThat(count(jdbc, "SELECT count(*) FROM business_worker_link WHERE recommendation_status = 'VERIFIED'")).isEqualTo(3);
        assertThat(count(jdbc, "SELECT count(*) FROM business_worker_link")).isEqualTo(5);
        assertThat(count(jdbc, "SELECT count(*) FROM provider_rate")).isEqualTo(7);
        assertThat(count(jdbc, "SELECT count(*) FROM business_review")).isZero();

        MarketplaceDemoSeedRunner.Result second = new MarketplaceDemoSeedRunner(ds, "87900 12345", LOGINS).seed();

        assertThat(second.inserted()).isZero();
        assertThat(count(jdbc, "SELECT count(*) FROM business WHERE website = ?", TAG)).isEqualTo(8);
        assertThat(count(jdbc, "SELECT count(*) FROM business_media")).isEqualTo(26);
        assertThat(count(jdbc, "SELECT count(*) FROM business_worker_link")).isEqualTo(5);
    }

    private static int count(JdbcTemplate jdbc, String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
