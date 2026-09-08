package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.Otp;
import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = RefreshTokenRotationConcurrencyIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.refresh.expiration.days=30",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RefreshTokenRotationConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_auth_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    private final RefreshTokenServiceImpl refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    RefreshTokenRotationConcurrencyIntegrationTest(
            RefreshTokenServiceImpl refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void concurrentRefreshUsingSameTokenAllowsOnlyOneReplacementAndPreservesUnrelatedSessions(CapturedOutput output)
            throws Exception {
        User user = saveUser("primary@example.com", "+919876543210");
        User unrelatedUser = saveUser("other@example.com", "+919876543211");

        String rawToken = refreshTokenService.createRefreshToken(user, "ios-primary", "fcm-primary");
        String sameUserOtherDeviceRaw = refreshTokenService.createRefreshToken(user, "android-secondary", "fcm-secondary");
        String otherUserRaw = refreshTokenService.createRefreshToken(unrelatedUser, "ios-primary", "fcm-other");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<RefreshAttempt> task = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                return RefreshAttempt.success(refreshTokenService.rotateRefreshToken(rawToken));
            } catch (RuntimeException ex) {
                return RefreshAttempt.failure(ex);
            }
        };

        Future<RefreshAttempt> first = executor.submit(task);
        Future<RefreshAttempt> second = executor.submit(task);

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        RefreshAttempt firstResult = first.get(10, TimeUnit.SECONDS);
        RefreshAttempt secondResult = second.get(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        List<RefreshAttempt> attempts = List.of(firstResult, secondResult);
        assertThat(attempts).filteredOn(RefreshAttempt::succeeded).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.succeeded()).hasSize(1);
        assertThat(attempts.stream()
                .filter(attempt -> !attempt.succeeded())
                .findFirst()
                .orElseThrow()
                .error())
                .isInstanceOf(IllegalArgumentException.class);

        List<RefreshToken> storedTokens = refreshTokenRepository.findAll();
        assertThat(storedTokens).noneMatch(token -> rawToken.equals(token.getToken()));
        assertThat(storedTokens).noneMatch(token -> sameUserOtherDeviceRaw.equals(token.getToken()));
        assertThat(storedTokens).noneMatch(token -> otherUserRaw.equals(token.getToken()));
        assertThat(storedTokens).allMatch(token -> token.getToken().matches("^[0-9a-f]{64}$"));
        assertThat(output).doesNotContain(rawToken, sameUserOtherDeviceRaw, otherUserRaw);

        Long revokedOldPrimaryTokens = countTokens(user.getId(), "ios-primary", true);
        Long activePrimaryReplacementTokens = countTokens(user.getId(), "ios-primary", false);
        Long activeSameUserOtherDeviceTokens = countTokens(user.getId(), "android-secondary", false);
        Long activeOtherUserTokens = countTokens(unrelatedUser.getId(), "ios-primary", false);

        // The loser's "already revoked" branch revokes the WHOLE active
        // family for this device, not just the one replayed token - so the
        // winner's brand-new replacement is collateral damage too, leaving
        // 2 revoked rows (the original + the winner's replacement) and 0
        // still-active for "ios-primary". This is intentional: the backend
        // cannot distinguish "two legitimate concurrent calls raced" from
        // "an attacker replayed an already-superseded token", and the
        // standard defense is to always assume compromise. A well-behaved
        // client avoids ever triggering this by single-flighting its own
        // refresh calls (as this project's mobile client does) rather than
        // relying on the backend to tell the two cases apart.
        assertThat(revokedOldPrimaryTokens).isEqualTo(2L);
        assertThat(activePrimaryReplacementTokens).isEqualTo(0L);
        assertThat(activeSameUserOtherDeviceTokens).isEqualTo(1L);
        assertThat(activeOtherUserTokens).isEqualTo(1L);
    }

    private User saveUser(String email, String phoneNumber) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded");
        user.setPhoneNumber(phoneNumber);
        user.setVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        return userRepository.saveAndFlush(user);
    }

    private Long countTokens(Long userId, String deviceId, boolean revoked) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM refresh_tokens
                WHERE user_id = ?
                  AND device_id = ?
                  AND revoked = ?
                """,
                Long.class,
                userId,
                deviceId,
                revoked
        );
    }

    private record RefreshAttempt(RefreshTokenRotationResult result, RuntimeException error) {
        static RefreshAttempt success(RefreshTokenRotationResult result) {
            return new RefreshAttempt(result, null);
        }

        static RefreshAttempt failure(RuntimeException error) {
            return new RefreshAttempt(null, error);
        }

        boolean succeeded() {
            return result != null;
        }
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
    @Import({RefreshTokenServiceImpl.class, RefreshTokenFamilyRevoker.class})
    static class TestApplication {
    }
}
