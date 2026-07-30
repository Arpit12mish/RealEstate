package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "prod", "staging"})
@RequiredArgsConstructor
@Slf4j
public class TwilioVerifyClientImpl implements TwilioVerifyClient {

    private final TwilioProperties properties;

    @PostConstruct
    public void initialize() {
        log.info(
                "Initializing Twilio. accountSid={}, verifyServiceSid={}, authTokenPresent={}",
                mask(properties.getAccountSid()),
                safeTrim(properties.getVerifyServiceSid()),
                properties.getAuthToken() != null && !properties.getAuthToken().isBlank()
        );
        Twilio.init(safeTrim(properties.getAccountSid()), safeTrim(properties.getAuthToken()));
    }

    @Override
    public VerificationResult sendVerification(String phoneNumber) {
        Verification verification = Verification.creator(
                safeTrim(properties.getVerifyServiceSid()),
                phoneNumber,
                "sms"
        ).create();
        return new VerificationResult(verification.getSid(), verification.getStatus());
    }

    @Override
    public VerificationResult checkVerification(String phoneNumber, String code) {
        VerificationCheck check = VerificationCheck.creator(
                        safeTrim(properties.getVerifyServiceSid()),
                        code
                )
                .setTo(phoneNumber)
                .create();
        return new VerificationResult(check.getSid(), check.getStatus());
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
}
