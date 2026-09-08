package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.observability.LogSanitizer;
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

    private final LogSanitizer logSanitizer;

    public FakeOtpService(LogSanitizer logSanitizer) {
        this.logSanitizer = logSanitizer;
    }

    @Override
    public OtpSendResult sendOtp(String phoneNumber) {
        log.info("FAKE OTP send accepted (no real SMS sent)");

        return OtpSendResult.builder()
                .status("OTP_SENT")
                .message("OTP sent successfully")
                .resendAfterSeconds(30)
                .build();
    }

    @Override
    public OtpVerificationResult verifyOtp(String phoneNumber, String code) {
        log.info("FAKE OTP verification attempted");
        return OtpVerificationResult.builder()
                .approved("123456".equals(code))
                .normalizedPhoneNumber(PhoneNumberNormalizer.normalize(phoneNumber))
                .build();
    }
}
