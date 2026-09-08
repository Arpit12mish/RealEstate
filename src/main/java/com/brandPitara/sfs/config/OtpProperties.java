package com.brandPitara.sfs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code sfs.otp.*} from application.yml. Tunable send/verify abuse
 * limits for TwilioOtpServiceImpl - previously hardcoded constants there.
 * {@code expiresInSeconds} is informational only: Twilio Verify owns the
 * actual code lifetime (a fixed ~10 minutes per Twilio's documented
 * behavior, not configurable via the Verify API), so this value only tells
 * the frontend what to render as a countdown and must be kept in sync by
 * hand if that provider default ever changes.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sfs.otp")
public class OtpProperties {

    private long resendCooldownSeconds = 30;
    private int maxSendsPerWindow = 5;
    private long sendWindowMinutes = 15;
    private int maxVerifyFailuresPerWindow = 5;
    private long verifyWindowMinutes = 10;
    private long blockMinutes = 15;
    private long expiresInSeconds = 600;
}
