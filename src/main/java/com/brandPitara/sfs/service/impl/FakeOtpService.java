package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
// @Profile("dev")
public class FakeOtpService implements OtpService {

    @Override
    public void sendOtp(String phoneNumber) {
        // No real SMS in dev
        String fixedCode = "123456";
        log.info("FAKE OTP: sending {} to phone {} (no real SMS sent)", fixedCode, phoneNumber);
        // optional: store in DB if your flow expects an Otp row
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String code) {
        log.info("FAKE OTP verify for {} with code {}", phoneNumber, code);
        // simplest rule: one fixed code; you can make it more flexible later
        return "123456".equals(code);
    }
}
