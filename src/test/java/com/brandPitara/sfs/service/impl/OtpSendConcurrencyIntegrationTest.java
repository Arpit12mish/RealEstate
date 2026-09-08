package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.OtpProperties;
import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.OtpMetrics;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.twilio.exception.ApiConnectionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the send-otp reservation race is closed: two concurrent sendOtp calls
 * for the same phone, with only one MAX_SENDS_PER_WINDOW slot left, must
 * result in exactly one Twilio call and exactly one success - not two of
 * either. Before the fix, TwilioOtpServiceImpl#prepareSend only checked the
 * limit and released the tracker row lock before calling Twilio;
 * TwilioOtpServiceImpl#reserveSendAttempt now checks AND reserves
 * (increments sendCountInWindow, sets cooldownUntil) atomically under that
 * same lock, so the loser observes the winner's reservation and is rejected
 * before ever reaching Twilio.
 */
@SpringBootTest(
        classes = OtpSendConcurrencyIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "sfs.log.dir=target/test-logs",
                "twilio.account-sid=ACtest",
                "twilio.auth-token=test-token",
                "twilio.verify-service-sid=VAtest",
                "app.review.enabled=false"
        }
)
@ActiveProfiles({"test", "dev"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OtpSendConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_otp_send_test")
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

    @MockitoBean
    private TwilioVerifyClient twilioVerifyClient;

    @Autowired
    private OtpRequestTrackerRepository trackerRepository;

    @Test
    void onlyOneOfTwoConcurrentSendsSucceedsWhenExactlyOneSlotRemains() throws Exception {
        String phoneNumber = "+919900011001";
        // MAX_SENDS_PER_WINDOW is 5; seed the tracker at 4 with no active
        // cooldown so the very next reservation is the last one available -
        // isolates the count-based race from the cooldown-based one.
        OffsetDateTime now = OffsetDateTime.now();
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(4);
        tracker.setSendWindowStart(now);
        tracker.setFailedVerifyCountInWindow(0);
        trackerRepository.save(tracker);

        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenReturn(new TwilioVerifyClient.VerificationResult("VE-race-test", "pending"));

        int attempts = 2;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<OutcomeOrRejection>> futures = IntStream.range(0, attempts)
                .mapToObj(i -> executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        return new OutcomeOrRejection(otpService.sendOtp(phoneNumber), null);
                    } catch (ResponseStatusException ex) {
                        return new OutcomeOrRejection(null, ex);
                    }
                }))
                .collect(Collectors.toList());

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (Future<OutcomeOrRejection> future : futures) {
            OutcomeOrRejection outcome = future.get(10, TimeUnit.SECONDS);
            if (outcome.result() != null) {
                assertThat(outcome.result().getStatus()).isEqualTo("OTP_SENT");
                succeeded.incrementAndGet();
            } else {
                assertThat(outcome.rejection().getStatusCode().value()).isEqualTo(429);
                rejected.incrementAndGet();
            }
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        // The core regression check: Twilio must be invoked exactly once, not
        // twice - the old code let both concurrent requests reach Twilio.
        verify(twilioVerifyClient, times(1)).sendVerification(phoneNumber);

        OtpRequestTracker persisted = trackerRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(persisted.getSendCountInWindow()).isEqualTo(5);
    }

    @Test
    void concurrentSendsForDifferentPhonesAllSucceedIndependently() throws Exception {
        int phoneCount = 25;
        String[] phones = IntStream.range(0, phoneCount)
                .mapToObj(i -> String.format("+9199%08d", 20000 + i))
                .toArray(String[]::new);

        when(twilioVerifyClient.sendVerification(anyString()))
                .thenReturn(new TwilioVerifyClient.VerificationResult("VE-multi-phone", "pending"));

        ExecutorService executor = Executors.newFixedThreadPool(phoneCount);
        CountDownLatch ready = new CountDownLatch(phoneCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<OtpSendResult>> futures = IntStream.range(0, phoneCount)
                .mapToObj(i -> executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return otpService.sendOtp(phones[i]);
                }))
                .collect(Collectors.toList());

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        for (Future<OtpSendResult> future : futures) {
            assertThat(future.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo("OTP_SENT");
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        verify(twilioVerifyClient, times(phoneCount)).sendVerification(anyString());
    }

    @Test
    void twilioConnectionFailureMapsTo503WithoutLeavingTheSlotUnreserved() {
        String phoneNumber = "+919900011099";
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenThrow(new ApiConnectionException("simulated timeout"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> otpService.sendOtp(phoneNumber))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));

        // The reservation (count + cooldown) already happened before the Twilio
        // call, so a provider failure still consumes the slot - a retry storm
        // against a struggling Twilio cannot bypass the rate limit either.
        OtpRequestTracker persisted = trackerRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(persisted.getSendCountInWindow()).isEqualTo(1);
        assertThat(persisted.getCooldownUntil()).isNotNull();
    }

    private record OutcomeOrRejection(OtpSendResult result, ResponseStatusException rejection) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties({TwilioProperties.class, AppReviewLoginProperties.class, OtpProperties.class})
    @EntityScan(basePackageClasses = OtpRequestTracker.class)
    @EnableJpaRepositories(
            basePackageClasses = OtpRequestTrackerRepository.class,
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.brandPitara\\.sfs\\.repository\\.(?!OtpRequestTrackerRepository$).*"
            )
    )
    @Import({TwilioOtpServiceImpl.class, LogSanitizer.class, OtpMetrics.class, ExternalProviderTransactions.class})
    static class TestApplication {
    }
}
