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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces, at the backend layer, the exact scenario from the mobile
 * refresh-token-persistence incident (RCA 2026-08-17): a client that always
 * advances to the newly-issued refresh token must be able to rotate
 * indefinitely, while a client that resends an already-rotated token (the
 * production bug) must be rejected and must lose the whole device session,
 * not just the replayed token. Uses the same self-contained slice context
 * (RefreshTokenServiceImpl + its two repositories only) as
 * {@link RefreshTokenRotationConcurrencyIntegrationTest} so it doesn't pull
 * in the rest of the application context.
 */
@SpringBootTest(
        classes = RefreshTokenSequentialRotationIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.refresh.expiration.days=30",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RefreshTokenSequentialRotationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_auth_sequential_test")
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

    @Autowired
    RefreshTokenSequentialRotationIntegrationTest(
            RefreshTokenServiceImpl refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Test
    void threeSequentialRotationsEachRevokeThePreviousTokenAndActivateExactlyOneReplacement() {
        User user = saveUser("sequential@example.com", "+919876500001");
        String deviceId = "ios-sequential";
        String r1 = refreshTokenService.createRefreshToken(user, deviceId, "fcm-sequential");

        RefreshTokenRotationResult rotation1 = refreshTokenService.rotateRefreshToken(r1);
        String r2 = rotation1.getRefreshToken();
        assertThat(r2).isNotEqualTo(r1);
        assertThat(findByRawToken(r1).isRevoked()).isTrue();
        assertThat(findByRawToken(r2).isRevoked()).isFalse();
        assertActiveCount(user.getId(), deviceId, 1);

        RefreshTokenRotationResult rotation2 = refreshTokenService.rotateRefreshToken(r2);
        String r3 = rotation2.getRefreshToken();
        assertThat(r3).isNotIn(r1, r2);
        assertThat(findByRawToken(r2).isRevoked()).isTrue();
        assertThat(findByRawToken(r3).isRevoked()).isFalse();
        assertActiveCount(user.getId(), deviceId, 1);

        RefreshTokenRotationResult rotation3 = refreshTokenService.rotateRefreshToken(r3);
        String r4 = rotation3.getRefreshToken();
        assertThat(r4).isNotIn(r1, r2, r3);
        assertThat(findByRawToken(r3).isRevoked()).isTrue();
        assertThat(findByRawToken(r4).isRevoked()).isFalse();
        assertActiveCount(user.getId(), deviceId, 1);

        // Four rows total for this device (R1..R4): three revoked, one
        // active - never a gap (0 active) and never a fork (>1 active).
        List<RefreshToken> deviceTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && deviceId.equals(t.getDeviceId()))
                .toList();
        assertThat(deviceTokens).hasSize(4);
        assertThat(deviceTokens).filteredOn(RefreshToken::isRevoked).hasSize(3);
        assertThat(deviceTokens).filteredOn(t -> !t.isRevoked()).hasSize(1);
    }

    @Test
    void replayingAnAlreadyRotatedTokenIsRejectedAndRevokesTheCurrentReplacementToo() {
        // This is the exact production bug from the mobile RCA: a client
        // that keeps resending R1 after the backend already rotated it to
        // R2 must be rejected, AND R2 - the token the backend actually
        // considers current - must be revoked as part of the same reuse
        // response. If R2 were left active, "reuse detection" would just be
        // a confusing error the legitimate client eventually recovers from
        // instead of the family-wide session kill it's meant to be.
        User user = saveUser("reuse@example.com", "+919876500002");
        String deviceId = "ios-reuse";
        String r1 = refreshTokenService.createRefreshToken(user, deviceId, "fcm-reuse");

        RefreshTokenRotationResult rotation = refreshTokenService.rotateRefreshToken(r1);
        String r2 = rotation.getRefreshToken();
        assertThat(findByRawToken(r2).isRevoked()).isFalse();

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(r1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reuse");

        assertThat(findByRawToken(r1).isRevoked()).isTrue();
        assertThat(findByRawToken(r2).isRevoked())
                .as("the currently-valid descendant must be revoked too - this is the family-wide "
                        + "protection that actually ends the session, not just a rejection of the replayed token")
                .isTrue();
        assertActiveCount(user.getId(), deviceId, 0);

        // The device is now genuinely locked out until a fresh login - even
        // r2 (also dead now) is rejected the same way on a further attempt.
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(r2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUnrelatedDevicesSessionSurvivesAReuseEventOnAnotherDevice() {
        User user = saveUser("multidevice@example.com", "+919876500003");
        String primaryDeviceId = "ios-primary";
        String secondaryDeviceId = "android-secondary";

        String primaryR1 = refreshTokenService.createRefreshToken(user, primaryDeviceId, "fcm-primary");
        String secondaryR1 = refreshTokenService.createRefreshToken(user, secondaryDeviceId, "fcm-secondary");

        refreshTokenService.rotateRefreshToken(primaryR1);
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(primaryR1))
                .isInstanceOf(IllegalArgumentException.class);

        assertActiveCount(user.getId(), primaryDeviceId, 0);
        assertActiveCount(user.getId(), secondaryDeviceId, 1);
        assertThat(findByRawToken(secondaryR1).isRevoked()).isFalse();
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

    private RefreshToken findByRawToken(String rawToken) {
        String hash = sha256Hex(rawToken);
        return refreshTokenRepository.findAll().stream()
                .filter(t -> t.getToken().equals(hash))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No stored token matches the given raw value"));
    }

    private void assertActiveCount(Long userId, String deviceId, int expected) {
        long active = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId)
                        && deviceId.equals(t.getDeviceId())
                        && !t.isRevoked())
                .count();
        assertThat(active).isEqualTo(expected);
    }

    private static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
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
