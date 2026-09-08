package com.brandPitara.sfs.scheduler;

import com.brandPitara.sfs.entity.Otp;
import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.impl.RefreshTokenServiceImpl;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = RefreshTokenCleanupPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false",
                "spring.datasource.hikari.maximum-pool-size=3",
                "spring.datasource.hikari.minimum-idle=3",
                "spring.datasource.hikari.connection-timeout=3000",
                "spring.task.scheduling.enabled=false",
                "jwt.refresh.expiration.days=30",
                "app.logging.path=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RefreshTokenCleanupPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_refresh_cleanup_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private RefreshTokenCleanupBatchWorker worker;
    @Autowired
    private RefreshTokenServiceImpl refreshTokenService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private HikariDataSource dataSource;
    @Autowired
    private MeterRegistry meterRegistry;

    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens RESTART IDENTITY");
        userIds.clear();
        for (int i = 0; i < 8; i++) {
            userIds.add(saveUser(i).getId());
        }
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_device_active
                ON refresh_tokens (user_id, device_id, revoked, expires_at)
                """);
    }

    @AfterEach
    void connectionPoolReturnsToIdle() throws InterruptedException {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((pool.getActiveConnections() != 0 || pool.getThreadsAwaitingConnection() != 0)
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(pool.getActiveConnections()).isZero();
        assertThat(pool.getThreadsAwaitingConnection()).isZero();
    }

    @Test
    void singleBatchNeverDeletesMoreThanConfiguredBoundaryAndProtectsValidTokens() {
        insertTokens("expired", 1_500, false, OffsetDateTime.now().minusDays(1));
        insertTokens("valid", 200, false, OffsetDateTime.now().plusDays(10));

        int deleted = worker.deleteBatch(OffsetDateTime.now(), 1_000);

        assertThat(deleted).isEqualTo(1_000);
        assertThat(countCleanupCandidates()).isEqualTo(500);
        assertThat(countValidTokens()).isEqualTo(200);
    }

    @Test
    void completedBatchRemainsCommittedWhenNextBatchInvocationFails() {
        insertTokens("expired", 1_500, false, OffsetDateTime.now().minusDays(1));

        assertThat(worker.deleteBatch(OffsetDateTime.now(), 1_000)).isEqualTo(1_000);
        assertThatThrownBy(() -> worker.deleteBatch(OffsetDateTime.now(), 10_001))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(countCleanupCandidates()).isEqualTo(500);
        assertThat(countAllTokens()).isEqualTo(500);
    }

    @Test
    void multipleIndependentBatchesCompleteCleanupIncludingRevokedUnexpiredRows() {
        insertTokens("expired", 1_100, false, OffsetDateTime.now().minusDays(1));
        insertTokens("revoked-future", 1_400, true, OffsetDateTime.now().plusDays(10));
        insertTokens("valid", 250, false, OffsetDateTime.now().plusDays(10));

        List<Integer> batches = drain(1_000);

        assertThat(batches).containsExactly(1_000, 1_000, 500);
        assertThat(countCleanupCandidates()).isZero();
        assertThat(countValidTokens()).isEqualTo(250);
    }

    @Test
    void exactCutoffAndFutureNonRevokedTokensAreNeverDeleted() {
        OffsetDateTime cutoff = OffsetDateTime.now().withNano(0);
        insertTokens("old", 20, false, cutoff.minusSeconds(1));
        insertTokens("equal", 20, false, cutoff);
        insertTokens("future", 20, false, cutoff.plusSeconds(1));

        assertThat(worker.deleteBatch(cutoff, 100)).isEqualTo(20);
        assertThat(countAllTokens()).isEqualTo(40);
    }

    @Test
    void tenThousandAndHundredThousandRowFixturesUseBoundedPrimaryKeyPlan() {
        FixtureCounts tenThousand = insertRepresentativeFixture("ten-k", 10_000);
        assertThat(tenThousand).isEqualTo(new FixtureCounts(10_000, 6_000, 2_500, 2_000, 1_500, 4_000));
        assertBoundedPlanUsesPrimaryKey();

        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens RESTART IDENTITY");
        FixtureCounts hundredThousand = insertRepresentativeFixture("hundred-k", 100_000);
        assertThat(hundredThousand)
                .isEqualTo(new FixtureCounts(100_000, 60_000, 25_000, 20_000, 15_000, 40_000));
        assertBoundedPlanUsesPrimaryKey();

        long started = System.nanoTime();
        int deleted = worker.deleteBatch(OffsetDateTime.now(), 1_000);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(deleted).isEqualTo(1_000);
        assertThat(durationMillis).isLessThan(5_000);
        assertThat(countAllTokens()).isEqualTo(99_000);
    }

    @Test
    void authenticationRemainsAvailableDuringCleanupWithoutDeadlocksOrPoolTimeouts() throws Exception {
        insertRepresentativeFixture("concurrent", 100_000);
        User authUser = saveUser(99);
        List<String> refreshInputs = createRawTokens(authUser, "refresh", 12);
        List<String> logoutInputs = createRawTokens(authUser, "logout", 12);
        List<String> replacements = java.util.Collections.synchronizedList(new ArrayList<>());
        List<String> loginTokens = java.util.Collections.synchronizedList(new ArrayList<>());

        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        Counter timeoutCounter = meterRegistry.find("hikaricp.connections.timeout").counter();
        double initialTimeouts = timeoutCounter == null ? 0 : timeoutCounter.count();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        AtomicBoolean sampling = new AtomicBoolean(true);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Future<Integer> cleanup = executor.submit(() -> {
            awaitStart(ready, start);
            int deleted = 0;
            int batch;
            do {
                batch = worker.deleteBatch(OffsetDateTime.now(), 500);
                deleted += batch;
            } while (batch == 500);
            return deleted;
        });
        Future<Long> refresh = executor.submit(() -> timed(() -> {
            for (String rawToken : refreshInputs) {
                RefreshTokenRotationResult rotated = refreshTokenService.rotateRefreshToken(rawToken);
                replacements.add(rotated.getRefreshToken());
            }
        }, ready, start));
        Future<Long> login = executor.submit(() -> timed(() -> {
            for (int i = 0; i < 24; i++) {
                loginTokens.add(refreshTokenService.createRefreshToken(authUser, "login-" + i, null));
            }
        }, ready, start));
        Future<Long> logout = executor.submit(() -> timed(() -> {
            for (String rawToken : logoutInputs) {
                refreshTokenService.revokeToken(rawToken);
            }
        }, ready, start));
        Future<?> sampler = executor.submit(() -> {
            while (sampling.get()) {
                peakActive.accumulateAndGet(pool.getActiveConnections(), Math::max);
                peakPending.accumulateAndGet(pool.getThreadsAwaitingConnection(), Math::max);
                Thread.onSpinWait();
            }
        });

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        int cleaned = cleanup.get(30, TimeUnit.SECONDS);
        long refreshMillis = refresh.get(30, TimeUnit.SECONDS);
        long loginMillis = login.get(30, TimeUnit.SECONDS);
        long logoutMillis = logout.get(30, TimeUnit.SECONDS);
        sampling.set(false);
        sampler.get(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(cleaned).isGreaterThanOrEqualTo(40_000);
        assertThat(replacements).hasSize(12);
        assertThat(loginTokens).hasSize(24);
        assertThat(replacements).allSatisfy(raw -> assertThat(refreshTokenRepository.findByToken(hash(raw))).isPresent());
        assertThat(loginTokens).allSatisfy(raw -> assertThat(refreshTokenRepository.findByToken(hash(raw))).isPresent());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deadlocks FROM pg_stat_database WHERE datname = current_database()",
                Long.class
        )).isZero();
        if (timeoutCounter != null) {
            assertThat(timeoutCounter.count()).isEqualTo(initialTimeouts);
        }
        assertThat(pool.getThreadsAwaitingConnection()).isZero();

        System.out.printf(
                "refresh-cleanup concurrency cleaned=%d refreshMs=%d loginMs=%d logoutMs=%d peakActive=%d peakPending=%d%n",
                cleaned, refreshMillis, loginMillis, logoutMillis, peakActive.get(), peakPending.get()
        );
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for concurrent test start");
        }
    }

    private long timed(ThrowingRunnable action, CountDownLatch ready, CountDownLatch start) throws Exception {
        awaitStart(ready, start);
        long started = System.nanoTime();
        action.run();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private List<String> createRawTokens(User user, String devicePrefix, int count) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add(refreshTokenService.createRefreshToken(user, devicePrefix + "-" + i, null));
        }
        return tokens;
    }

    private List<Integer> drain(int batchSize) {
        List<Integer> batches = new ArrayList<>();
        int deleted;
        do {
            deleted = worker.deleteBatch(OffsetDateTime.now(), batchSize);
            if (deleted > 0) {
                batches.add(deleted);
            }
        } while (deleted == batchSize);
        return batches;
    }

    private FixtureCounts insertRepresentativeFixture(String prefix, int total) {
        Long[] ids = userIds.toArray(Long[]::new);
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO refresh_tokens(user_id, token, device_id, expires_at, revoked, created_at)
                    SELECT user_ids[1 + (g % cardinality(user_ids))],
                           md5(? || g::text) || md5('token-' || ? || g::text),
                           'device-' || (g % 8),
                           now() + CASE WHEN g % 4 = 0 THEN interval '-1 day' ELSE interval '15 days' END,
                           g % 5 = 0,
                           now()
                    FROM generate_series(1, ?) AS g,
                         (SELECT ?::bigint[] AS user_ids) fixture
                    """);
            statement.setString(1, prefix);
            statement.setString(2, prefix);
            statement.setInt(3, total);
            statement.setArray(4, connection.createArrayOf("bigint", ids));
            return statement;
        });
        jdbcTemplate.execute("ANALYZE refresh_tokens");
        return fixtureCounts();
    }

    private void insertTokens(String prefix, int count, boolean revoked, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens(user_id, token, device_id, expires_at, revoked, created_at)
                SELECT ?, md5(? || g::text) || md5('token-' || ? || g::text),
                       'device-' || (g % 8), ?, ?, now()
                FROM generate_series(1, ?) AS g
                """, userIds.get(0), prefix, prefix, expiresAt, revoked, count);
    }

    private void assertBoundedPlanUsesPrimaryKey() {
        String plan = String.join("\n", jdbcTemplate.queryForList("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT id
                FROM refresh_tokens
                WHERE expires_at < now() OR revoked = true
                ORDER BY id
                LIMIT 1000
                FOR UPDATE SKIP LOCKED
                """, String.class));
        assertThat(plan).contains("Index Scan using refresh_tokens_pkey");
        System.out.println("refresh-cleanup bounded plan:\n" + plan);
    }

    private FixtureCounts fixtureCounts() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE NOT revoked AND expires_at >= now()) AS valid,
                       count(*) FILTER (WHERE expires_at < now()) AS expired,
                       count(*) FILTER (WHERE revoked) AS revoked,
                       count(*) FILTER (WHERE revoked AND expires_at >= now()) AS revoked_unexpired,
                       count(*) FILTER (WHERE expires_at < now() OR revoked) AS candidates
                FROM refresh_tokens
                """, (rs, rowNum) -> new FixtureCounts(
                rs.getInt("total"),
                rs.getInt("valid"),
                rs.getInt("expired"),
                rs.getInt("revoked"),
                rs.getInt("revoked_unexpired"),
                rs.getInt("candidates")
        ));
    }

    private long countCleanupCandidates() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE expires_at < now() OR revoked",
                Long.class
        );
    }

    private long countValidTokens() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE expires_at >= now() AND NOT revoked",
                Long.class
        );
    }

    private long countAllTokens() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens", Long.class);
    }

    private User saveUser(int suffix) {
        String unique = suffix + "-" + UUID.randomUUID();
        User user = new User();
        user.setEmail(unique + "@example.com");
        user.setPassword("encoded");
        user.setPhoneNumber("+91" + String.format("%010d", Math.abs(unique.hashCode()) % 10_000_000_000L));
        user.setVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        return userRepository.saveAndFlush(user);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record FixtureCounts(
            int total,
            int valid,
            int expired,
            int revoked,
            int revokedUnexpired,
            int candidates
    ) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {User.class, RefreshToken.class, Otp.class})
    @EnableJpaRepositories(
            basePackageClasses = {UserRepository.class, RefreshTokenRepository.class},
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.brandPitara\\.sfs\\.repository\\.(?!(RefreshTokenRepository|UserRepository)$).*"
            )
    )
    @Import({RefreshTokenCleanupBatchWorker.class, RefreshTokenServiceImpl.class})
    static class TestApplication {
    }
}
