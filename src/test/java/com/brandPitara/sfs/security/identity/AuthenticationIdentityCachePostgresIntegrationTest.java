package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.config.JwtRequestFilter;
import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetailsService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.entity.Otp;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.AppUserDetailsService;
import com.brandPitara.sfs.service.UserPhoneLookupService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Comparator;
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

@SpringBootTest(
        classes = AuthenticationIdentityCachePostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "spring.flyway.enabled=false",
                "spring.datasource.hikari.maximum-pool-size=3",
                "spring.datasource.hikari.minimum-idle=3",
                "spring.datasource.hikari.connection-timeout=3000",
                "spring.task.scheduling.enabled=false",
                "jwt.secret=release-4a-mobile-secret-key-release-4a-mobile-secret-key",
                "jwt.expiration.ms=3600000",
                "dashboard.jwt.secret=release-4a-dashboard-secret-key-release-4a-dashboard-secret-key",
                "dashboard.jwt.access-expiration-ms=3600000",
                "sfs.authentication.identity-cache.mobile.enabled=true",
                "sfs.authentication.identity-cache.mobile.maximum-size=100",
                "sfs.authentication.identity-cache.mobile.expire-after-write=30s",
                "sfs.authentication.identity-cache.dashboard.enabled=true",
                "sfs.authentication.identity-cache.dashboard.maximum-size=100",
                "sfs.authentication.identity-cache.dashboard.expire-after-write=30s",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationIdentityCachePostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_auth_identity_cache")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired private UserRepository userRepository;
    @Autowired private DashboardUserRepository dashboardUserRepository;
    @Autowired private AppUserDetailsService appUserDetailsService;
    @Autowired private DashboardUserDetailsService dashboardUserDetailsService;
    @Autowired private MobileAuthenticationIdentityCache mobileCache;
    @Autowired private DashboardAuthenticationIdentityCache dashboardCache;
    @Autowired private AuthenticationIdentityCacheInvalidator invalidator;
    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private DashboardJwtService dashboardJwtService;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private HikariDataSource dataSource;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private JwtRequestFilter mobileFilter;
    private DashboardJwtAuthenticationFilter dashboardFilter;

    @BeforeEach
    void setUp() {
        mobileFilter = new JwtRequestFilter(
                appUserDetailsService,
                jwtTokenUtil,
                new LogSanitizer(),
                mobileCache
        );
        dashboardFilter = new DashboardJwtAuthenticationFilter(
                dashboardJwtService,
                dashboardUserDetailsService,
                new LogSanitizer()
        );
    }

    @Test
    void sequentialAndMultipleUserRequestsHaveBoundedIdentityQueries() throws Exception {
        User one = saveMobileUser(Role.CUSTOMER, true);
        String oneToken = mobileToken(one);
        mobileCache.invalidate(one.getId());
        Statistics statistics = statistics();
        statistics.clear();
        List<Long> oneUserDurations = runSequential(100, i -> authenticateMobile(oneToken));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(oneUserDurations).hasSize(100);

        List<User> users = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = saveMobileUser(Role.CUSTOMER, true);
            users.add(user);
            tokens.add(mobileToken(user));
            mobileCache.invalidate(user.getId());
        }
        statistics.clear();
        List<Long> multipleDurations = runSequential(100, i -> authenticateMobile(tokens.get(i % 10)));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(10);
        printLatency("mobile-sequential-one", oneUserDurations);
        printLatency("mobile-multiple-users", multipleDurations);
    }

    @Test
    void concurrentColdAccessCoalescesLoadAndReturnsPoolToIdle() throws Exception {
        User user = saveMobileUser(Role.CUSTOMER, true);
        String token = mobileToken(user);
        mobileCache.invalidate(user.getId());
        statistics().clear();
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        Counter timeouts = meterRegistry.find("hikaricp.connections.timeout").counter();
        double initialTimeouts = timeouts == null ? 0 : timeouts.count();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        AtomicBoolean sampling = new AtomicBoolean(true);
        ExecutorService executor = Executors.newFixedThreadPool(101);
        CountDownLatch ready = new CountDownLatch(100);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                long started = System.nanoTime();
                assertThat(authenticateMobile(token)).isNotNull();
                return System.nanoTime() - started;
            }));
        }
        Future<?> sampler = executor.submit(() -> {
            while (sampling.get()) {
                peakActive.accumulateAndGet(pool.getActiveConnections(), Math::max);
                peakPending.accumulateAndGet(pool.getThreadsAwaitingConnection(), Math::max);
                Thread.onSpinWait();
            }
        });

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<Long> durations = new ArrayList<>();
        for (Future<Long> future : futures) {
            durations.add(future.get(15, TimeUnit.SECONDS));
        }
        sampling.set(false);
        sampler.get(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        awaitPoolIdle(pool);
        assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
        assertThat(pool.getActiveConnections()).isZero();
        assertThat(pool.getThreadsAwaitingConnection()).isZero();
        if (timeouts != null) {
            assertThat(timeouts.count()).isEqualTo(initialTimeouts);
        }
        printLatency("mobile-concurrent-one", durations);
        System.out.printf(
                "identity-cache concurrency peakActive=%d peakPending=%d timeouts=0%n",
                peakActive.get(), peakPending.get()
        );
    }

    @Test
    void guestRequestsNeverPerformIdentityLookup() throws Exception {
        String guestToken = jwtTokenUtil.generateGuestToken(1L, "guest-installation");
        statistics().clear();

        List<Long> durations = runSequential(100, i -> authenticateMobile(guestToken));

        assertThat(statistics().getPrepareStatementCount()).isZero();
        assertThat(durations).hasSize(100);
        printLatency("guest", durations);
    }

    @Test
    void dashboardCacheIsIndependentAndLoadsEachDashboardUserOnce() throws Exception {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DashboardUserEntity user = saveDashboardUser(DashboardRole.ADMIN, true);
            tokens.add(dashboardJwtService.generateAccessToken(user));
            dashboardCache.invalidate(user.getId());
        }
        statistics().clear();

        List<Long> durations = runSequential(100, i -> authenticateDashboard(tokens.get(i % 10)));

        assertThat(statistics().getPrepareStatementCount()).isEqualTo(10);
        printLatency("dashboard", durations);
    }

    @Test
    void disableEnableRoleChangeAndDeletionAreVisibleImmediatelyAfterInvalidation() throws Exception {
        User user = saveMobileUser(Role.CUSTOMER, true);
        String token = mobileToken(user);
        assertThat(authenticateMobile(token)).isNotNull();

        mutateMobile(user.getId(), "UPDATE users SET is_verified = false WHERE id = ?");
        assertThat(authenticateMobile(token)).isNull();

        mutateMobile(user.getId(), "UPDATE users SET is_verified = true WHERE id = ?");
        Authentication enabled = authenticateMobile(token);
        assertThat(enabled).isNotNull();
        assertThat(enabled.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");

        mutateMobile(user.getId(), "UPDATE users SET role = 'WORKER' WHERE id = ?");
        Authentication roleChanged = authenticateMobile(token);
        assertThat(roleChanged).isNotNull();
        assertThat(roleChanged.getAuthorities()).extracting("authority").containsExactly("ROLE_WORKER");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());
            invalidator.invalidateMobileAfterCommit(user.getId());
        });
        assertThat(authenticateMobile(token)).isNull();
    }

    @Test
    void dashboardDisableAndRoleChangeAreVisibleAfterNamespaceInvalidation() throws Exception {
        DashboardUserEntity user = saveDashboardUser(DashboardRole.ADMIN, true);
        String token = dashboardJwtService.generateAccessToken(user);
        assertThat(authenticateDashboard(token)).isNotNull();

        mutateDashboard(user.getId(), "UPDATE dashboard_users SET active = false WHERE id = ?");
        assertThat(authenticateDashboard(token)).isNull();

        mutateDashboard(user.getId(), "UPDATE dashboard_users SET active = true, role = 'REVIEWER' WHERE id = ?");
        Authentication changed = authenticateDashboard(token);
        assertThat(changed).isNotNull();
        assertThat(changed.getAuthorities()).extracting("authority").containsExactly("ROLE_REVIEWER");
    }

    @Test
    void missingUserTokenIsDeniedWithoutAuthenticationBypass() throws Exception {
        String token = jwtTokenUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        "+919999999999", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                ),
                Long.MAX_VALUE,
                "+919999999999",
                "ADMIN"
        );

        assertThat(authenticateMobile(token)).isNull();
    }

    private void mutateMobile(Long userId, String sql) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.update(sql, userId);
            invalidator.invalidateMobileAfterCommit(userId);
        });
    }

    private void mutateDashboard(Long userId, String sql) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbcTemplate.update(sql, userId);
            invalidator.invalidateDashboardAfterCommit(userId);
        });
    }

    private Authentication authenticateMobile(String token) throws Exception {
        return authenticate(mobileFilter, "/api/private", token);
    }

    private Authentication authenticateDashboard(String token) throws Exception {
        return authenticate(dashboardFilter, "/api/dashboard/private", token);
    }

    private Authentication authenticate(jakarta.servlet.Filter filter, String uri, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.addHeader("Authorization", "Bearer " + token);
        Authentication[] captured = new Authentication[1];
        try {
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                    captured[0] = SecurityContextHolder.getContext().getAuthentication()
            );
            return captured[0];
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<Long> runSequential(int count, ThrowingIndexedAction action) throws Exception {
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long started = System.nanoTime();
            assertThat(action.run(i)).isNotNull();
            durations.add(System.nanoTime() - started);
        }
        return durations;
    }

    private User saveMobileUser(Role role, boolean verified) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setEmail(unique + "@example.com");
        user.setPhoneNumber("+91" + unique.substring(0, 10).replaceAll("[a-f]", "7"));
        user.setPassword("password-hash-must-not-enter-cache");
        user.setVerified(verified);
        user.setRole(role);
        user.setOnboardingStatus(OnboardingStatus.CUSTOMER_READY);
        return userRepository.saveAndFlush(user);
    }

    private DashboardUserEntity saveDashboardUser(DashboardRole role, boolean active) {
        String unique = UUID.randomUUID().toString();
        return dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Dashboard User")
                .email(unique + "@example.com")
                .passwordHash("dashboard-password-hash-must-not-enter-cache")
                .role(role)
                .active(active)
                .build());
    }

    private String mobileToken(User user) {
        return jwtTokenUtil.generateToken(
                new MobileAuthenticationUserSnapshot(user.getId(), user.getPhoneNumber(), user.getRole(), true),
                user.getId(),
                user.getPhoneNumber(),
                user.getRole().name()
        );
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void awaitPoolIdle(HikariPoolMXBean pool) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((pool.getActiveConnections() != 0 || pool.getThreadsAwaitingConnection() != 0)
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    private void printLatency(String workload, List<Long> durations) {
        durations.sort(Comparator.naturalOrder());
        System.out.printf(
                "identity-cache %s p50Ms=%.3f p95Ms=%.3f%n",
                workload,
                durations.get(49) / 1_000_000.0,
                durations.get(94) / 1_000_000.0
        );
    }

    @FunctionalInterface
    private interface ThrowingIndexedAction {
        Authentication run(int index) throws Exception;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {User.class, Otp.class, DashboardUserEntity.class})
    @EnableJpaRepositories(
            basePackageClasses = {UserRepository.class, DashboardUserRepository.class},
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.brandPitara\\.sfs\\.(repository|dashboard\\.user\\.repository)\\.(?!(UserRepository|DashboardUserRepository)$).*"
            )
    )
    @Import({
            AuthenticationIdentityCacheProperties.class,
            MobileAuthenticationIdentityCache.class,
            DashboardAuthenticationIdentityCache.class,
            AuthenticationIdentityCacheInvalidator.class,
            AppUserDetailsService.class,
            UserPhoneLookupService.class,
            DashboardUserDetailsService.class,
            JwtTokenUtil.class,
            DashboardJwtService.class
    })
    static class TestApplication {
    }
}
