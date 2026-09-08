package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.TwilioOtpProviderCondition;
import com.brandPitara.sfs.config.TwilioProperties;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import com.twilio.Twilio;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/** Must share TwilioOtpServiceImpl's exact wiring condition: it is TwilioOtpServiceImpl's
 * only constructor dependency, so if these two ever diverge, whichever profile the
 * condition disagrees on fails to start with a NoSuchBeanDefinitionException. */
@Component
@Conditional(TwilioOtpProviderCondition.class)
@RequiredArgsConstructor
@Slf4j
public class TwilioVerifyClientImpl implements TwilioVerifyClient {

    private final TwilioProperties properties;

    @PostConstruct
    public void initialize() {
        log.info(
                "Initializing Twilio. accountSid={}, verifyServiceSid={}, authTokenPresent={}, " +
                        "connectTimeoutMs={}, readTimeoutMs={}",
                mask(properties.getAccountSid()),
                safeTrim(properties.getVerifyServiceSid()),
                properties.getAuthToken() != null && !properties.getAuthToken().isBlank(),
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs()
        );

        // The SDK's default NetworkHttpClient has no read timeout, so a Twilio-side
        // hang would otherwise block the calling Tomcat thread indefinitely. This
        // call happens outside any DB transaction, but an unbounded wait here can
        // still exhaust the servlet thread pool during a provider outage.
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(properties.getConnectTimeoutMs())
                .setConnectionRequestTimeout(properties.getConnectTimeoutMs())
                .setSocketTimeout(properties.getReadTimeoutMs())
                .build();

        TwilioRestClient restClient = new TwilioRestClient.Builder(
                safeTrim(properties.getAccountSid()),
                safeTrim(properties.getAuthToken())
        ).httpClient(new NetworkHttpClient(requestConfig)).build();

        Twilio.setRestClient(restClient);
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
