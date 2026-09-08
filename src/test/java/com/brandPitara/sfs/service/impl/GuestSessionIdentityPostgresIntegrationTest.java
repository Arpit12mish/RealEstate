package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.GuestSessionRequest;
import com.brandPitara.sfs.entity.GuestIdentityLink;
import com.brandPitara.sfs.entity.GuestSession;
import com.brandPitara.sfs.entity.Otp;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.repository.GuestIdentityLinkRepository;
import com.brandPitara.sfs.repository.GuestSessionRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = GuestSessionIdentityPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuestSessionIdentityPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_guest_identity_test")
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
    private GuestSessionServiceImpl service;

    @Autowired
    private GuestSessionRepository guestSessionRepository;

    @Autowired
    private GuestIdentityLinkRepository identityLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void enforceProductionActiveEpochConstraint() {
        identityLinkRepository.deleteAll();
        guestSessionRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_guest_sessions_one_active_epoch_per_installation
                ON guest_sessions (installation_id)
                WHERE active = TRUE AND linked_user_id IS NULL
                """);
    }

    @Test
    void sameInstallationKeepsIndependentGuestEpochsAndImmutableHistoricalLinks() {
        String installationId = "shared-installation";
        User userA = saveUser("+919876500101");
        User userB = saveUser("+919876500102");

        Long guestAId = service.createOrReuseGuestSession(request(installationId)).getGuestSessionId();
        service.linkGuestSessionToUser(installationId, userA);

        Long guestBId = service.createOrReuseGuestSession(request(installationId)).getGuestSessionId();
        assertThat(guestBId).isNotEqualTo(guestAId);
        assertThat(service.createOrReuseGuestSession(request(installationId)).getGuestSessionId())
                .as("an unconverted guest survives an app restart")
                .isEqualTo(guestBId);

        service.linkGuestSessionToUser(installationId, userB);

        GuestSession guestA = guestSessionRepository.findById(guestAId).orElseThrow();
        GuestSession guestB = guestSessionRepository.findById(guestBId).orElseThrow();
        assertThat(guestA.isActive()).isFalse();
        assertThat(guestA.getLinkedUser().getId()).isEqualTo(userA.getId());
        assertThat(guestB.isActive()).isFalse();
        assertThat(guestB.getLinkedUser().getId()).isEqualTo(userB.getId());

        List<GuestIdentityLink> links = identityLinkRepository.findAll();
        assertThat(links).hasSize(2);
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getGuestSession().getId()).isEqualTo(guestAId);
            assertThat(link.getUser().getId()).isEqualTo(userA.getId());
        });
        assertThat(links).anySatisfy(link -> {
            assertThat(link.getGuestSession().getId()).isEqualTo(guestBId);
            assertThat(link.getUser().getId()).isEqualTo(userB.getId());
        });
    }

    @Test
    void concurrentConversionsCreateExactlyOneLinkAndNeverRelinkTheGuest() throws Exception {
        String installationId = "concurrent-installation";
        User userA = saveUser("+919876500103");
        User userB = saveUser("+919876500104");
        Long guestId = service.createOrReuseGuestSession(request(installationId)).getGuestSessionId();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                await(start);
                service.linkGuestSessionToUser(installationId, userA);
            });
            Future<?> second = executor.submit(() -> {
                await(start);
                service.linkGuestSessionToUser(installationId, userB);
            });

            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        GuestSession converted = guestSessionRepository.findById(guestId).orElseThrow();
        List<GuestIdentityLink> links = identityLinkRepository.findAll();
        assertThat(converted.isActive()).isFalse();
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getGuestSession().getId()).isEqualTo(guestId);
        assertThat(converted.getLinkedUser().getId()).isEqualTo(links.get(0).getUser().getId());
    }

    private GuestSessionRequest request(String installationId) {
        GuestSessionRequest request = new GuestSessionRequest();
        request.setInstallationId(installationId);
        request.setPlatform("ANDROID");
        request.setAppVersion("1.0.0");
        return request;
    }

    private User saveUser(String phone) {
        User user = new User();
        user.setEmail("phone_" + phone.substring(1) + "@phone.local");
        user.setPassword("encoded");
        user.setPhoneNumber(phone);
        user.setVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);
        return userRepository.saveAndFlush(user);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {GuestSession.class, GuestIdentityLink.class, User.class, Otp.class})
    @EnableJpaRepositories(
            basePackageClasses = GuestSessionRepository.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.brandPitara\\.sfs\\.repository\\.(?!(GuestSessionRepository|GuestIdentityLinkRepository|UserRepository)$).*"
            )
    )
    @Import(GuestSessionServiceImpl.class)
    static class TestApplication {
        @Bean
        JwtTokenUtil jwtTokenUtil() {
            JwtTokenUtil jwt = mock(JwtTokenUtil.class);
            when(jwt.generateGuestToken(anyLong(), anyString())).thenReturn("guest-jwt");
            return jwt;
        }
    }
}
