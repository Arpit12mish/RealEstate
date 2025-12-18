package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.service.OtpService;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"prod", "staging"})
public class TwilioOtpServiceImpl implements OtpService {

    private final TwilioProperties props;

    @PostConstruct
    public void initTwilio() {
        log.info("Initializing Twilio with accountSid={}", props.getAccountSid());
        Twilio.init(props.getAccountSid(), props.getAuthToken());
    }

    @Override
    public void sendOtp(String phoneNumber) {
        try {
            log.info("Sending OTP via Twilio to {}", phoneNumber);

            Verification verification = Verification.creator(
                            props.getVerifyServiceSid(),
                            phoneNumber,
                            "sms")
                    .create();

            log.info("Twilio verification created: sid={}, status={}",
                    verification.getSid(), verification.getStatus());

        } catch (ApiException e) {
            // Twilio-specific error
            log.error("Twilio API error while sending OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(), e.getCode(), e.getMessage(), e);

            // Expose Twilio’s message in the response so Postman can show it
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
        try {
            log.info("Verifying OTP for {}", phoneNumber);

            VerificationCheck check = VerificationCheck.creator(
                            props.getVerifyServiceSid(),
                            code)
                    .setTo(phoneNumber)
                    .create();

            log.info("Twilio verification check: sid={}, status={}",
                    check.getSid(), check.getStatus());

            return "approved".equalsIgnoreCase(check.getStatus());

        } catch (ApiException e) {
            log.error("Twilio API error while verifying OTP: statusCode={}, code={}, message={}",
                    e.getStatusCode(), e.getCode(), e.getMessage(), e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Twilio error: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error while verifying OTP", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error while verifying OTP"
            );
        }
    }
}
