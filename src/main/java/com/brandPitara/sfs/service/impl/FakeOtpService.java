package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.LocalFakeOtpProperties;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "local-fake-otp", "local-staging"})
@ConditionalOnProperty(
        prefix = "sfs.local-staging.fake-otp",
        name = "enabled",
        havingValue = "true"
)
public class FakeOtpService implements OtpService {

    /** Mirrors TwilioOtpServiceImpl's real-provider default (Twilio Verify's fixed code TTL); purely informational here since no OTP is actually sent. */
    private static final long FAKE_EXPIRES_IN_SECONDS = 600;

    private final LocalFakeOtpProperties properties;

    public FakeOtpService(LocalFakeOtpProperties properties) {
        this.properties = properties;
    }

    @Override
    public OtpSendResult sendOtp(String phoneNumber) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber);
        log.info("Local OTP request handled without external SMS delivery");

        return OtpSendResult.builder()
                .status("OTP_SENT")
                .message("OTP sent successfully")
                .resendAfterSeconds(properties.getResendAfterSeconds())
                .expiresInSeconds(FAKE_EXPIRES_IN_SECONDS)
                .normalizedPhoneNumber(normalizedPhone)
                .build();
    }

    @Override
    public OtpVerificationResult verifyOtp(String phoneNumber, String code) {
        log.info("FAKE OTP verification attempted");
        String normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber);
        return OtpVerificationResult.builder()
                .approved(properties.matches(normalizedPhone, code))
                .normalizedPhoneNumber(normalizedPhone)
                .build();
    }
}
