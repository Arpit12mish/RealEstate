package com.brandPitara.sfs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
// import org.springframework.context.annotation.Configuration;

// @Configuration
@ConfigurationProperties(prefix = "twilio")
@Getter
@Setter
public class TwilioProperties {

    private String accountSid;
    private String authToken;
    private String verifyServiceSid;
    private String phoneNumber;

    private String verifyTemplateSid;

    /**
     * Bounded HTTP timeouts for the Twilio SDK's REST client. Without these, the
     * SDK's default Apache HttpClient has no read timeout, so a Twilio-side hang
     * can block a Tomcat request thread indefinitely - at high OTP volume during
     * a Twilio outage this can exhaust the servlet thread pool even though no
     * database connection is ever held during the call.
     */
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 8000;
}
