package com.brandPitara.sfs.config;

import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@ConfigurationProperties(prefix = "sfs.local-staging.fake-otp")
public class LocalFakeOtpProperties {

    private boolean enabled;
    private String fixedOtp;
    private Set<String> phoneNumbers = Set.of();
    private long resendAfterSeconds = 30;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFixedOtp(String fixedOtp) {
        this.fixedOtp = fixedOtp;
    }

    public void setPhoneNumbers(Set<String> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            this.phoneNumbers = Set.of();
            return;
        }

        Set<String> normalizedNumbers = new LinkedHashSet<>();
        for (String phoneNumber : phoneNumbers) {
            normalizedNumbers.add(PhoneNumberNormalizer.normalize(phoneNumber));
        }
        this.phoneNumbers = Set.copyOf(normalizedNumbers);
    }

    public void setResendAfterSeconds(long resendAfterSeconds) {
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public boolean matches(String phoneNumber, String code) {
        if (!enabled || fixedOtp == null || code == null || phoneNumber == null) {
            return false;
        }

        String normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber);
        return phoneNumbers.contains(normalizedPhone) && fixedOtp.equals(code);
    }
}
