package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.entity.OtpRequestTracker;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.rest.verify.v2.service.VerificationCreator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "prod", "staging"})
@Transactional
public class TwilioOtpServiceImpl implements OtpService {

    private static final long RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_SENDS_PER_WINDOW = 5;
    private static final long SEND_WINDOW_MINUTES = 15;
    private static final int MAX_VERIFY_FAILURES_PER_WINDOW = 5;
    private static final long VERIFY_WINDOW_MINUTES = 10;
    private static final long BLOCK_MINUTES = 15;

    private final TwilioProperties props;
    private final AppReviewLoginProperties reviewLoginProperties;
    private final OtpRequestTrackerRepository trackerRepository;

    @PostConstruct
    public void initTwilio() {
        log.info(
                "Initializing Twilio. accountSid={}, verifyServiceSid={}, authTokenPresent={}",
                mask(props.getAccountSid()),
                safeTrim(props.getVerifyServiceSid()),
                props.getAuthToken() != null && !props.getAuthToken().isBlank()
        );

        Twilio.init(
                safeTrim(props.getAccountSid()),
                safeTrim(props.getAuthToken())
        );
    }

    @Override
    public OtpSendResult sendOtp(String phoneNumber) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        if (reviewLoginProperties.matchesPhone(normalizedPhone)) {
            log.info("Apple review OTP bypass send accepted for configured review number");
            return OtpSendResult.builder()
                    .status("OTP_SENT")
                    .message("OTP sent successfully")
                    .resendAfterSeconds(reviewLoginProperties.getResendAfterSeconds())
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OtpRequestTracker tracker = trackerRepository.findByPhoneNumber(normalizedPhone)
                .orElseGet(() -> createNewTracker(normalizedPhone));

        enforceBlockIfAny(tracker, now);
        resetSendWindowIfNeeded(tracker, now);

        if (tracker.getCooldownUntil() != null && now.isBefore(tracker.getCooldownUntil())) {
            long waitSeconds = Duration.between(now, tracker.getCooldownUntil()).getSeconds();
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait " + Math.max(waitSeconds, 1) + " seconds before requesting OTP again"
            );
        }

        if (tracker.getSendCountInWindow() >= MAX_SENDS_PER_WINDOW) {
            tracker.setBlockedUntil(now.plusMinutes(BLOCK_MINUTES));
            trackerRepository.save(tracker);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again later"
            );
        }

        try {
            log.info("Sending OTP via Twilio to {}", maskPhone(normalizedPhone));

            VerificationCreator creator = Verification.creator(
                    safeTrim(props.getVerifyServiceSid()),
                    normalizedPhone,
                    "sms"
            );

            Verification verification = creator.create();

            log.info(
                    "Twilio verification created: sid={}, status={}",
                    verification.getSid(),
                    verification.getStatus()
            );

            tracker.setLastSentAt(now);
            tracker.setCooldownUntil(now.plusSeconds(RESEND_COOLDOWN_SECONDS));
            tracker.setSendCountInWindow(tracker.getSendCountInWindow() + 1);

            if (tracker.getSendWindowStart() == null) {
                tracker.setSendWindowStart(now);
            }

            trackerRepository.save(tracker);

            return OtpSendResult.builder()
                    .status("OTP_SENT")
                    .message("OTP sent successfully")
                    .resendAfterSeconds(RESEND_COOLDOWN_SECONDS)
                    .build();

        } catch (ApiException e) {
            log.error(
                    "Twilio API error while sending OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Twilio error: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error while sending OTP", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error while sending OTP"
            );
        }
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String code) {
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        if (reviewLoginProperties.matchesPhone(normalizedPhone)) {
            boolean approved = reviewLoginProperties.matchesOtp(code);

            if (!approved) {
                log.warn("Apple review OTP bypass failed because fixed OTP did not match");
            } else {
                log.info("Apple review OTP bypass verified successfully");
            }

            return approved;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Optional<OtpRequestTracker> optionalTracker = trackerRepository.findByPhoneNumber(normalizedPhone);
        OtpRequestTracker tracker = optionalTracker.orElseGet(() -> createNewTracker(normalizedPhone));

        enforceBlockIfAny(tracker, now);
        resetVerifyWindowIfNeeded(tracker, now);

        try {
            log.info("Verifying OTP via Twilio for {}", maskPhone(normalizedPhone));

            VerificationCheck check = VerificationCheck.creator(
                            safeTrim(props.getVerifyServiceSid()),
                            code
                    )
                    .setTo(normalizedPhone)
                    .create();

            log.info(
                    "Twilio verification check: sid={}, status={}",
                    check.getSid(),
                    check.getStatus()
            );

            boolean approved = "approved".equalsIgnoreCase(check.getStatus());

            if (approved) {
                tracker.setFailedVerifyCountInWindow(0);
                tracker.setVerifyWindowStart(null);
                tracker.setBlockedUntil(null);
                tracker.setLastVerifiedAt(now);
                trackerRepository.save(tracker);
                return true;
            }

            increaseVerifyFailure(tracker, now);
            return false;

        } catch (ApiException e) {
            log.error(
                    "Twilio API error while verifying OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );

            increaseVerifyFailure(tracker, now);
            return false;

        } catch (Exception e) {
            log.error("Unexpected error while verifying OTP", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error while verifying OTP"
            );
        }
    }

    private OtpRequestTracker createNewTracker(String phoneNumber) {
        OtpRequestTracker tracker = new OtpRequestTracker();
        tracker.setPhoneNumber(phoneNumber);
        tracker.setSendCountInWindow(0);
        tracker.setFailedVerifyCountInWindow(0);
        return tracker;
    }

    private void enforceBlockIfAny(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getBlockedUntil() != null && now.isBefore(tracker.getBlockedUntil())) {
            long waitMinutes = Duration.between(now, tracker.getBlockedUntil()).toMinutes();
            long safeMinutes = Math.max(waitMinutes, 1);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please try again in " + safeMinutes + " minute(s)"
            );
        }
    }

    private void resetSendWindowIfNeeded(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getSendWindowStart() == null ||
                Duration.between(tracker.getSendWindowStart(), now).toMinutes() >= SEND_WINDOW_MINUTES) {
            tracker.setSendWindowStart(now);
            tracker.setSendCountInWindow(0);
        }
    }

    private void resetVerifyWindowIfNeeded(OtpRequestTracker tracker, OffsetDateTime now) {
        if (tracker.getVerifyWindowStart() == null ||
                Duration.between(tracker.getVerifyWindowStart(), now).toMinutes() >= VERIFY_WINDOW_MINUTES) {
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

        if (failures >= MAX_VERIFY_FAILURES_PER_WINDOW) {
            tracker.setBlockedUntil(now.plusMinutes(BLOCK_MINUTES));
        }

        trackerRepository.save(tracker);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        String trimmed = phoneNumber.trim().replaceAll("\\s+", "");

        if (!trimmed.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number must be in E.164 format like +919876543210"
            );
        }

        return trimmed;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String mask(String value) {
        if (value == null || value.length() < 8) {
            return "null-or-short";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "masked";
        }
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 4);
    }
}