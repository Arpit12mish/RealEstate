package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "local-fake-otp"})
public class FakeOtpService implements OtpService {

    @Override
    public OtpSendResult sendOtp(String phoneNumber) {
        String fixedCode = "123456";
        log.info("FAKE OTP: sending {} to phone {} (no real SMS sent)", fixedCode, phoneNumber);

        return OtpSendResult.builder()
                .status("OTP_SENT")
                .message("OTP sent successfully")
                .resendAfterSeconds(30)
                .build();
    }

    @Override
    public OtpVerificationResult verifyOtp(String phoneNumber, String code) {
        log.info("FAKE OTP verify for {} with code {}", phoneNumber, code);
        return OtpVerificationResult.builder()
                .approved("123456".equals(code))
                .normalizedPhoneNumber(PhoneNumberNormalizer.normalize(phoneNumber))
                .build();
    }
}
