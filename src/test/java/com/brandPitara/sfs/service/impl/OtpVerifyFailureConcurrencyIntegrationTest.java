package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that concurrent failed verify-otp attempts for the same phone number cannot
 * lose updates to failedVerifyCountInWindow. Before the fix, TwilioOtpServiceImpl read
 * the tracker via a plain (unlocked) findByPhoneNumber, incremented in Java, and saved —
 * a classic read-modify-write race that let parallel wrong-OTP attempts bypass
 * MAX_VERIFY_FAILURES_PER_WINDOW. TwilioOtpServiceImpl#recordFailedVerifyAttempt now
 * fetches the tracker with a PESSIMISTIC_WRITE lock (OtpRequestTrackerRepository
 * #findByPhoneNumberForUpdate), so this test exercises that path directly without
 * depending on the real Twilio API.
 */
@SpringBootTest(
        classes = OtpVerifyFailureConcurrencyIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "app.logging.path=target/test-logs",
                "twilio.account-sid=ACtest",
                "twilio.auth-token=test-token",
                "twilio.verify-service-sid=VAtest",
                "app.review.enabled=false"
        }
)
@ActiveProfiles({"test", "dev"})
@Testcontainers(disabledWithoutDocker = true)
class OtpVerifyFailureConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_otp_test")
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
    private TwilioOtpServiceImpl otpService;

    @Autowired
    private OtpRequestTrackerRepository trackerRepository;

    @Test
    void concurrentFailedVerifyAttemptsDoNotLoseUpdatesOrExceedBlockThreshold() throws Exception {
        String phoneNumber = "+919876543210";
        int attempts = 8; // MAX_VERIFY_FAILURES_PER_WINDOW is 5; drive well past it.

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<OtpRequestTracker>> futures = IntStream.range(0, attempts)
                .mapToObj(i -> executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return otpService.recordFailedVerifyAttempt(phoneNumber);
                }))
                .collect(Collectors.toList());

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        for (Future<OtpRequestTracker> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        OtpRequestTracker persisted = trackerRepository.findByPhoneNumber(phoneNumber).orElseThrow();

        // Every one of the `attempts` concurrent increments must be reflected exactly once;
        // a lost update would leave this below `attempts`.
        assertThat(persisted.getFailedVerifyCountInWindow()).isEqualTo(attempts);
        // 8 failures is past the 5-failure threshold, so the account must end up blocked.
        assertThat(persisted.getBlockedUntil()).isNotNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties({TwilioProperties.class, AppReviewLoginProperties.class})
    @EntityScan(basePackageClasses = OtpRequestTracker.class)
    @EnableJpaRepositories(basePackageClasses = OtpRequestTrackerRepository.class)
    @Import({TwilioOtpServiceImpl.class, LogSanitizer.class})
    static class TestApplication {
    }
}
