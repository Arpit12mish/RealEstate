package com.brandPitara.sfs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.review")
public class AppReviewLoginProperties {

    private boolean enabled = false;
    private String phoneNumber;
    private String fixedOtp;
    private long resendAfterSeconds = 30;

    public boolean matchesPhone(String inputPhoneNumber) {
        if (!enabled || phoneNumber == null || inputPhoneNumber == null) {
            return false;
        }

        String configured = phoneNumber.trim().replaceAll("\\s+", "");
        String input = inputPhoneNumber.trim().replaceAll("\\s+", "");

        return configured.equals(input);
    }

    public boolean matchesOtp(String inputOtp) {
        if (!enabled || fixedOtp == null || inputOtp == null) {
            return false;
        }

        return fixedOtp.trim().equals(inputOtp.trim());
    }
}