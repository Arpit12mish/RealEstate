package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.OtpProperties;
import com.brandPitara.sfs.config.TwilioOtpProviderCondition;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.exception.OtpRequestException;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.OtpMetrics;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * The real OTP provider for dev/staging/prod (unconditionally), and for
 * local-staging whenever the local fixed-OTP bypass (FakeOtpService) is not
 * explicitly enabled. local-staging is included here - not just
 * dev/staging/prod - so that activating that profile alone never leaves zero
 * OtpService beans: TwilioOtpProviderCondition and FakeOtpService's own
 * ConditionalOnProperty(havingValue = "true") are complementary/exclusive
 * there, so exactly one OtpService bean exists for every supported profile
 * combination (see OtpProviderProfileRoutingTest).
 * <p>
 * Also the resend implementation: there is no separate resend code path.
 * Twilio Verify has no API-level distinction between an initial send and a
 * resend for the same phone number - the caller (AuthController's
 * /request-otp and /otp/resend routes) simply calls sendOtp again, and this
 * class's cooldown/window/block bookkeeping already treats every call
 * identically and atomically per phone number, which is what keeps the
 * abuse budget unified across both routes (see RateLimitPolicyResolver,
 * which maps both routes to the same MOBILE_OTP_REQUEST policy for the same
 * reason at the infra layer).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Conditional(TwilioOtpProviderCondition.class)
public class TwilioOtpServiceImpl implements OtpService {

    private final AppReviewLoginProperties reviewLoginProperties;
    private final OtpRequestTrackerRepository trackerRepository;
    private final LogSanitizer logSanitizer;
    private final TwilioVerifyClient twilioVerifyClient;
    private final ExternalProviderTransactions externalProviderTransactions;
    private final OtpProperties otpProperties;
    private final OtpMetrics otpMetrics;

    @Override
    public OtpSendResult sendOtp(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        if (reviewLoginProperties.matchesPhone(normalizedPhone)) {
            log.info("Apple review OTP bypass send accepted for configured review number");
            return OtpSendResult.builder()
                    .status("OTP_SENT")
                    .message("OTP sent successfully")
                    .resendAfterSeconds(reviewLoginProperties.getResendAfterSeconds())
                    .expiresInSeconds(otpProperties.getExpiresInSeconds())
                    .normalizedPhoneNumber(normalizedPhone)
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Checks the send-window/cooldown/block limits AND reserves this attempt
        // (increments sendCountInWindow, sets cooldownUntil) atomically under the
        // tracker row lock, all before the Twilio call. Reserving up front - not
        // after Twilio responds - is what makes this safe under concurrency: two
        // requests for the same phone are serialized by the row lock, so the
        // second one sees the first one's reservation and is rejected before it
        // ever reaches Twilio. It also means a failed/timed-out Twilio call still
        // consumes the slot and cooldown, so retries can't turn into a storm.
        reserveSendAttempt(normalizedPhone, now);

        try {
            log.info("Sending OTP via Twilio to {}", logSanitizer.maskPhone(normalizedPhone));

            TwilioVerifyClient.VerificationResult verification =
                    twilioVerifyClient.sendVerification(normalizedPhone);

            log.info(
                    "Twilio verification created: sid={}, status={}",
                    verification.sid(),
                    verification.status()
            );
            // The attempt is already reserved; this only records the timestamp,
            // via a single-column update that needs no lock and cannot clobber a
            // concurrent transaction's changes to the other tracker columns. A
            // @Modifying query still needs an active transaction to execute at
            // all (Spring Data does not open one implicitly here), hence the
            // short wrapper - caught by OtpSendConcurrencyIntegrationTest
            // actually exercising a real repository/transaction manager instead
            // of a mock.
            externalProviderTransactions.write(() ->
                    trackerRepository.updateLastSentAt(normalizedPhone, now));

            otpMetrics.sendSuccess();
            return OtpSendResult.builder()
                    .status("OTP_SENT")
                    .message("OTP sent successfully")
                    .resendAfterSeconds(otpProperties.getResendCooldownSeconds())
                    .expiresInSeconds(otpProperties.getExpiresInSeconds())
                    .normalizedPhoneNumber(normalizedPhone)
                    .build();

        } catch (ApiConnectionException e) {
            log.error("Twilio connection failure while sending OTP: {}", e.getMessage(), e);
            otpMetrics.sendFailed("provider_unavailable");

            throw new OtpRequestException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP_PROVIDER_UNAVAILABLE",
                    "OTP provider temporarily unavailable, please try again shortly",
                    null
            );
        } catch (ApiException e) {
            log.error(
                    "Twilio API error while sending OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );
            otpMetrics.sendFailed("provider_error");

            throw new OtpRequestException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_PROVIDER_ERROR",
                    "Twilio error: " + e.getMessage(),
                    null
            );
        } catch (Exception e) {
            log.error("Unexpected error while sending OTP", e);
            otpMetrics.sendFailed("unexpected");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error while sending OTP"
            );
        }
    }

    @Override
    public OtpVerificationResult verifyOtp(String phoneNumber, String code) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        if (reviewLoginProperties.matchesPhone(normalizedPhone)) {
            boolean approved = reviewLoginProperties.matchesOtp(code);

            if (!approved) {
                log.warn("Apple review OTP bypass failed because fixed OTP did not match");
            } else {
                log.info("Apple review OTP bypass verified successfully");
            }

            return OtpVerificationResult.builder()
                    .approved(approved)
                    .normalizedPhoneNumber(normalizedPhone)
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        prepareVerification(normalizedPhone, now);

        try {
            log.info("Verifying OTP via Twilio for {}", logSanitizer.maskPhone(normalizedPhone));

            TwilioVerifyClient.VerificationResult check =
                    twilioVerifyClient.checkVerification(normalizedPhone, code);

            log.info(
                    "Twilio verification check: sid={}, status={}",
                    check.sid(),
                    check.status()
            );

            boolean approved = "approved".equalsIgnoreCase(check.status());

            if (approved) {
                recordVerificationSuccess(normalizedPhone, now);
                otpMetrics.verifySuccess();
                return OtpVerificationResult.builder()
                        .approved(true)
                        .normalizedPhoneNumber(normalizedPhone)
                        .build();
            }

            recordVerificationFailure(normalizedPhone, now);
            otpMetrics.verifyFailed();
            return OtpVerificationResult.builder()
                    .approved(false)
                    .normalizedPhoneNumber(normalizedPhone)
                    .build();

        } catch (ApiConnectionException e) {
            // A provider connectivity failure is not the caller's fault - unlike a
            // wrong code, it must not count against failedVerifyCountInWindow.
            log.error("Twilio connection failure while verifying OTP: {}", e.getMessage(), e);

            throw new OtpRequestException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP_PROVIDER_UNAVAILABLE",
                    "OTP provider temporarily unavailable, please try again shortly",
                    null
            );
        } catch (ApiException e) {
            log.error(
                    "Twilio API error while verifying OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );

            recordVerificationFailure(normalizedPhone, now);
            otpMetrics.verifyFailed();
            return OtpVerificationResult.builder()
                    .approved(false)
                    .normalizedPhoneNumber(normalizedPhone)
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error while verifying OTP", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error while verifying OTP"
            );
        }
    }

    /**
     * Checks the send limits and reserves this attempt (cooldown + window count)
     * in one locked transaction. Concurrent callers for the same phone are
     * serialized by the PESSIMISTIC_WRITE row lock in findOrCreateLocked: the
     * loser re-reads the winner's committed reservation and is correctly
     * rejected here, before either one has called Twilio.
     */
    private void reserveSendAttempt(String phoneNumber, OffsetDateTime now) {
        externalProviderTransactions.write(() -> {
            OtpRequestTracker tracker = findOrCreateLocked(phoneNumber);
            enforceBlockIfAny(tracker, now, "send");
            resetSendWindowIfNeeded(tracker, now);

            if (tracker.getCooldownUntil() != null && now.isBefore(tracker.getCooldownUntil())) {
                long waitSeconds = Math.max(Duration.between(now, tracker.getCooldownUntil()).getSeconds(), 1);
                otpMetrics.sendFailed("cooldown");
                throw new OtpRequestException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP_RESEND_TOO_SOON",
                        "Please wait before requesting another OTP.",
                        waitSeconds
                );
            }

            if (tracker.getSendCountInWindow() >= otpProperties.getMaxSendsPerWindow()) {
                tracker.setBlockedUntil(now.plusMinutes(otpProperties.getBlockMinutes()));
                trackerRepository.save(tracker);
                otpMetrics.sendFailed("limit_exceeded");
                throw new OtpRequestException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP_REQUEST_LIMIT_EXCEEDED",
                        "Too many OTP requests. Please try again later.",
                        Duration.ofMinutes(otpProperties.getBlockMinutes()).toSeconds()
                );
            }

            tracker.setCooldownUntil(now.plusSeconds(otpProperties.getResendCooldownSeconds()));
            tracker.setSendCountInWindow(tracker.getSendCountInWindow() + 1);
            if (tracker.getSendWindowStart() == null) {
                tracker.setSendWindowStart(now);
            }
            trackerRepository.save(tracker);
        });
    }

    private void prepareVerification(String phoneNumber, OffsetDateTime now) {
        externalProviderTransactions.write(() -> {
            OtpRequestTracker tracker = findOrCreateLocked(phoneNumber);
            enforceBlockIfAny(tracker, now, "verify");
            resetVerifyWindowIfNeeded(tracker, now);
        });
    }

    private void recordVerificationSuccess(String phoneNumber, OffsetDateTime now) {
        externalProviderTransactions.write(() -> {
            OtpRequestTracker tracker = findOrCreateLocked(phoneNumber);
            tracker.setFailedVerifyCountInWindow(0);
            tracker.setVerifyWindowStart(null);
            tracker.setBlockedUntil(null);
            tracker.setLastVerifiedAt(now);
            trackerRepository.save(tracker);
        });
    }

    private void recordVerificationFailure(String phoneNumber, OffsetDateTime now) {
        externalProviderTransactions.write(() -> {
            OtpRequestTracker tracker = findOrCreateLocked(phoneNumber);
            resetVerifyWindowIfNeeded(tracker, now);
            increaseVerifyFailure(tracker, now);
        });
    }

    private OtpRequestTracker findOrCreateLocked(String phoneNumber) {
        Optional<OtpRequestTracker> existing = trackerRepository.findByPhoneNumberForUpdate(phoneNumber);
        if (existing.isPresent()) {
            return existing.get();
        }

        trackerRepository.insertIfAbsent(phoneNumber);
        return trackerRepository.findByPhoneNumberForUpdate(phoneNumber)
                .orElseThrow(() -> new IllegalStateException("OTP request tracker was not created"));
    }

    private void enforceBlockIfAny(OtpRequestTracker tracker, OffsetDateTime now, String context) {
        if (tracker.getBlockedUntil() != null && now.isBefore(tracker.getBlockedUntil())) {
            long waitSeconds = Math.max(Duration.between(now, tracker.getBlockedUntil()).getSeconds(), 1);
            long safeMinutes = Math.max(waitSeconds / 60, 1);

            otpMetrics.blocked(context);
            throw new OtpRequestException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_REQUEST_LIMIT_EXCEEDED",
                    "Too many attempts. Please try again in " + safeMinutes + " minute(s)",
                    waitSeconds
            );
        }
    }

    private void resetSendWindowIfNeeded(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getSendWindowStart() == null ||
                Duration.between(tracker.getSendWindowStart(), now).toMinutes() >= otpProperties.getSendWindowMinutes()) {
            tracker.setSendWindowStart(now);
            tracker.setSendCountInWindow(0);
        }
    }

    private void resetVerifyWindowIfNeeded(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getVerifyWindowStart() == null ||
                Duration.between(tracker.getVerifyWindowStart(), now).toMinutes() >= otpProperties.getVerifyWindowMinutes()) {
            tracker.setVerifyWindowStart(now);
            tracker.setFailedVerifyCountInWindow(0);
        }
    }

    private void increaseVerifyFailure(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getVerifyWindowStart() == null) {
            tracker.setVerifyWindowStart(now);
        }

        int failures = tracker.getFailedVerifyCountInWindow() + 1;
        tracker.setFailedVerifyCountInWindow(failures);

        if (failures >= otpProperties.getMaxVerifyFailuresPerWindow()) {
            tracker.setBlockedUntil(now.plusMinutes(otpProperties.getBlockMinutes()));
        }

        trackerRepository.save(tracker);
    }

    /**
     * Package-private seam for concurrency tests: reproduces the locked-fetch +
     * failure-increment sequence verifyOtp runs on a failed check, without depending on
     * a real Twilio API call. Exercises the same findByPhoneNumberForUpdate lock and
     * increaseVerifyFailure logic that prevents the lost-update race under concurrency.
     */
    OtpRequestTracker recordFailedVerifyAttempt(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return externalProviderTransactions.write(() -> {
            OtpRequestTracker tracker = findOrCreateLocked(normalizedPhone);
            resetVerifyWindowIfNeeded(tracker, now);
            increaseVerifyFailure(tracker, now);
            return tracker;
        });
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return PhoneNumberNormalizer.normalize(phoneNumber);
    }

}
