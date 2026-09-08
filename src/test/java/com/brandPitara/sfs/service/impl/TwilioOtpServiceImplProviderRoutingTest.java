package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.OtpProperties;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.OtpMetrics;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the App Store review OTP bypass invariant from AppReviewLoginProperties
 * is enforced purely by app.review.enabled + exact phone/OTP match - not by
 * Spring profile. The bypass exists specifically so Apple reviewers can log
 * into the real production app without a real SMS; gating it on "profile !=
 * prod" would disable it in the only environment it is for.
 */
@ExtendWith(MockitoExtension.class)
class TwilioOtpServiceImplProviderRoutingTest {

    @Mock
    private OtpRequestTrackerRepository trackerRepository;
    @Mock
    private TwilioVerifyClient twilioVerifyClient;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private TwilioOtpServiceImpl newService(AppReviewLoginProperties reviewProperties) {
        return new TwilioOtpServiceImpl(
                reviewProperties,
                trackerRepository,
                new LogSanitizer(),
                twilioVerifyClient,
                new ExternalProviderTransactions(transactionManager),
                new OtpProperties(),
                OtpMetrics.isolated()
        );
    }

    @Test
    void normalNumberStillUsesTwilioProvider() {
        String phoneNumber = "+919876543210";
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(0);
        tracker.setFailedVerifyCountInWindow(0);

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.empty(), Optional.of(tracker), Optional.of(tracker));
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenReturn(new TwilioVerifyClient.VerificationResult("VE-test", "pending"));

        TwilioOtpServiceImpl service = newService(new AppReviewLoginProperties());

        assertThat(service.sendOtp(phoneNumber).getStatus()).isEqualTo("OTP_SENT");

        verify(twilioVerifyClient).sendVerification(phoneNumber);
        verify(trackerRepository).insertIfAbsent(phoneNumber);
        verify(trackerRepository).save(tracker);
    }

    @Test
    void reviewBypassDisabledByDefaultStillRoutesMatchingPhoneThroughTwilio() {
        String phoneNumber = "+919811341410";
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(0);
        tracker.setFailedVerifyCountInWindow(0);

        // enabled defaults to false even though phone/OTP below would otherwise match.
        AppReviewLoginProperties reviewProperties = new AppReviewLoginProperties();
        reviewProperties.setPhoneNumber(phoneNumber);
        reviewProperties.setFixedOtp("123456");

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        when(trackerRepository.findByPhoneNumberForUpdate(phoneNumber))
                .thenReturn(Optional.empty(), Optional.of(tracker), Optional.of(tracker));
        when(twilioVerifyClient.sendVerification(phoneNumber))
                .thenReturn(new TwilioVerifyClient.VerificationResult("VE-disabled-test", "pending"));

        TwilioOtpServiceImpl service = newService(reviewProperties);

        assertThat(service.sendOtp(phoneNumber).getStatus()).isEqualTo("OTP_SENT");

        verify(twilioVerifyClient).sendVerification(phoneNumber);
    }

    @Test
    void productionUsesReviewBypassWhenExplicitlyEnabledWithExactMatch() {
        String phoneNumber = "+919811341410";

        AppReviewLoginProperties reviewProperties = new AppReviewLoginProperties();
        reviewProperties.setEnabled(true);
        reviewProperties.setPhoneNumber(phoneNumber);
        reviewProperties.setFixedOtp("123456");

        TwilioOtpServiceImpl service = newService(reviewProperties);

        assertThat(service.sendOtp(phoneNumber).getStatus()).isEqualTo("OTP_SENT");

        OtpVerificationResult verification = service.verifyOtp(phoneNumber, "123456");
        assertThat(verification.isApproved()).isTrue();

        verify(twilioVerifyClient, never()).sendVerification(any());
        verify(twilioVerifyClient, never()).checkVerification(any(), any());
        verify(trackerRepository, never()).findByPhoneNumberForUpdate(any());
    }

    @Test
    void reviewBypassRejectsWrongOtpEvenWithMatchingPhone() {
        String phoneNumber = "+919811341410";

        AppReviewLoginProperties reviewProperties = new AppReviewLoginProperties();
        reviewProperties.setEnabled(true);
        reviewProperties.setPhoneNumber(phoneNumber);
        reviewProperties.setFixedOtp("123456");

        TwilioOtpServiceImpl service = newService(reviewProperties);

        OtpVerificationResult verification = service.verifyOtp(phoneNumber, "000000");

        assertThat(verification.isApproved()).isFalse();
        verify(twilioVerifyClient, never()).checkVerification(any(), any());
    }
}
