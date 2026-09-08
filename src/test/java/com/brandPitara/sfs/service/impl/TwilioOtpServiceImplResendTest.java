package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.OtpProperties;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.exception.OtpRequestException;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.OtpMetrics;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the resend implementation added on top of the existing send-otp
 * bookkeeping: there is no separate resend code path (see
 * TwilioOtpServiceImpl's class javadoc) - a "resend" is just another call to
 * sendOtp() for the same phone, so these tests exercise sendOtp() directly
 * under the same cooldown/window/block tracker states a real resend would hit.
 */
@ExtendWith(MockitoExtension.class)
class TwilioOtpServiceImplResendTest {

    @Mock
    private OtpRequestTrackerRepository trackerRepository;
    @Mock
    private TwilioVerifyClient twilioVerifyClient;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private TwilioOtpServiceImpl newService(OtpProperties otpProperties) {
        return new TwilioOtpServiceImpl(
                new AppReviewLoginProperties(),
                trackerRepository,
                new LogSanitizer(),
                twilioVerifyClient,
                new ExternalProviderTransactions(transactionManager),
                otpProperties,
                OtpMetrics.isolated()
        );
    }

    @Test
    void resendBeforeCooldownElapsedIsRejectedWithStructuredRetryAfter() {
        String phoneNumber = "+919900000101";
        OffsetDateTime now = OffsetDateTime.now();
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(1);
        tracker.setSendWindowStart(now);
        tracker.setCooldownUntil(now.plusSeconds(18));

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.of(tracker));

        TwilioOtpServiceImpl service = newService(new OtpProperties());

        assertThatThrownBy(() -> service.sendOtp(phoneNumber))
                .isInstanceOf(OtpRequestException.class)
                .satisfies(ex -> {
                    OtpRequestException otpEx = (OtpRequestException) ex;
                    assertThat(otpEx.getStatusCode().value()).isEqualTo(429);
                    assertThat(otpEx.getCode()).isEqualTo("OTP_RESEND_TOO_SOON");
                    assertThat(otpEx.getRetryAfterSeconds()).isBetween(1L, 18L);
                });

        verify(twilioVerifyClient, never()).sendVerification(any());
    }

    @Test
    void resendAfterCooldownElapsedSucceedsAndReflectsConfiguredCooldown() {
        String phoneNumber = "+919900000102";
        OffsetDateTime past = OffsetDateTime.now().minusSeconds(60);
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(1);
        tracker.setSendWindowStart(past);
        tracker.setCooldownUntil(past.plusSeconds(30)); // already elapsed

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.of(tracker));
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenReturn(new TwilioVerifyClient.VerificationResult("VE-resend-ok", "pending"));

        OtpProperties otpProperties = new OtpProperties();
        otpProperties.setResendCooldownSeconds(45);
        otpProperties.setExpiresInSeconds(600);

        TwilioOtpServiceImpl service = newService(otpProperties);

        OtpSendResult result = service.sendOtp(phoneNumber);

        assertThat(result.getStatus()).isEqualTo("OTP_SENT");
        assertThat(result.getResendAfterSeconds()).isEqualTo(45);
        assertThat(result.getExpiresInSeconds()).isEqualTo(600);
        assertThat(result.getNormalizedPhoneNumber()).isEqualTo(phoneNumber);
        verify(twilioVerifyClient).sendVerification(phoneNumber);
    }

    @Test
    void resendPastConfiguredMaxSendsPerWindowIsBlockedWithStructuredCode() {
        String phoneNumber = "+919900000103";
        OffsetDateTime now = OffsetDateTime.now();
        OtpProperties otpProperties = new OtpProperties();
        otpProperties.setMaxSendsPerWindow(3);
        otpProperties.setBlockMinutes(20);

        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(3); // already at the configured max
        tracker.setSendWindowStart(now);

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.of(tracker));

        TwilioOtpServiceImpl service = newService(otpProperties);

        assertThatThrownBy(() -> service.sendOtp(phoneNumber))
                .isInstanceOf(OtpRequestException.class)
                .satisfies(ex -> {
                    OtpRequestException otpEx = (OtpRequestException) ex;
                    assertThat(otpEx.getStatusCode().value()).isEqualTo(429);
                    assertThat(otpEx.getCode()).isEqualTo("OTP_REQUEST_LIMIT_EXCEEDED");
                    assertThat(otpEx.getRetryAfterSeconds()).isEqualTo(20 * 60L);
                });

        assertThat(tracker.getBlockedUntil()).isNotNull();
        verify(twilioVerifyClient, never()).sendVerification(any());
    }

    @Test
    void twilioConnectionFailureMapsToStructuredProviderUnavailable() {
        String phoneNumber = "+919900000104";
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.empty(), Optional.of(newTracker(phoneNumber)));
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenThrow(new ApiConnectionException("simulated timeout"));

        TwilioOtpServiceImpl service = newService(new OtpProperties());

        assertThatThrownBy(() -> service.sendOtp(phoneNumber))
                .isInstanceOf(OtpRequestException.class)
                .satisfies(ex -> {
                    OtpRequestException otpEx = (OtpRequestException) ex;
                    assertThat(otpEx.getStatusCode().value()).isEqualTo(503);
                    assertThat(otpEx.getCode()).isEqualTo("OTP_PROVIDER_UNAVAILABLE");
                });
    }

    @Test
    void twilioApiErrorMapsToStructuredProviderError() {
        String phoneNumber = "+919900000105";
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.empty(), Optional.of(newTracker(phoneNumber)));
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenThrow(new ApiException("invalid destination"));

        TwilioOtpServiceImpl service = newService(new OtpProperties());

        assertThatThrownBy(() -> service.sendOtp(phoneNumber))
                .isInstanceOf(OtpRequestException.class)
                .satisfies(ex -> {
                    OtpRequestException otpEx = (OtpRequestException) ex;
                    assertThat(otpEx.getStatusCode().value()).isEqualTo(400);
                    assertThat(otpEx.getCode()).isEqualTo("OTP_PROVIDER_ERROR");
                });
    }

    private OtpRequestTracker newTracker(String phoneNumber) {
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(0);
        tracker.setFailedVerifyCountInWindow(0);
        return tracker;
    }
}
