package com.brandPitara.sfs.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * True when either:
 * <ul>
 *   <li>the active profile is dev/prod/staging - Twilio is unconditionally the
 *       OTP provider there, regardless of sfs.local-staging.fake-otp.enabled
 *       (a local-staging-only concept that must have zero effect elsewhere),
 *       or</li>
 *   <li>the active profile is local-staging AND
 *       sfs.local-staging.fake-otp.enabled is not "true" - local-staging
 *       falls back to real Twilio unless the fixed-OTP bypass was explicitly
 *       turned on, so activating local-staging alone never leaves zero
 *       OtpService beans.</li>
 * </ul>
 * A plain {@code @Profile + @ConditionalOnProperty} pair on the bean cannot
 * express this OR-of-two-different-condition-types, since annotations stacked
 * on one class combine with AND.
 */
public class TwilioOtpProviderCondition implements Condition {

    private static final String FAKE_OTP_ENABLED_PROPERTY = "sfs.local-staging.fake-otp.enabled";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();

        if (environment.acceptsProfiles(Profiles.of("dev", "prod", "staging"))) {
            return true;
        }

        if (environment.acceptsProfiles(Profiles.of("local-staging"))) {
            return !environment.getProperty(FAKE_OTP_ENABLED_PROPERTY, Boolean.class, false);
        }

        return false;
    }
}
