package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.AppReviewLoginProperties;
import com.brandPitara.sfs.config.LocalFakeOtpProperties;
import com.brandPitara.sfs.config.OtpProperties;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.OtpMetrics;
import com.brandPitara.sfs.repository.OtpRequestTrackerRepository;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.TwilioVerifyClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Proves which OtpService implementation is actually wired per active profile
 * combination, and - critically - that exactly one is ever constructed, never
 * zero and never two. FakeOtpService{local, local-fake-otp, local-staging}
 * requires sfs.local-staging.fake-otp.enabled=true; TwilioOtpServiceImpl{dev,
 * prod, staging, local-staging} requires that same property to be anything
 * other than "true" (including unset). local-staging is deliberately in both
 * @Profile sets - the ConditionalOnProperty pair is what keeps them mutually
 * exclusive there, so activating local-staging alone (without remembering to
 * also set the fake-otp flag) still boots with a real OtpService bean instead
 * of failing with NoSuchBeanDefinitionException.
 */
class OtpProviderProfileRoutingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FakeOtpService.class, TwilioOtpServiceImpl.class)
            .withBean(LogSanitizer.class)
            .withBean(LocalFakeOtpProperties.class)
            .withBean(AppReviewLoginProperties.class)
            .withBean(OtpProperties.class)
            .withBean(OtpMetrics.class, OtpMetrics::isolated)
            .withBean(OtpRequestTrackerRepository.class, () -> mock(OtpRequestTrackerRepository.class))
            .withBean(TwilioVerifyClient.class, () -> mock(TwilioVerifyClient.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(ExternalProviderTransactions.class);

    @ParameterizedTest
    @ValueSource(strings = {"local", "local-fake-otp", "local-staging"})
    void localProfilesUseFakeOtpAndNeverConstructTwilioProvider(String profile) {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=" + profile,
                        "sfs.local-staging.fake-otp.enabled=true",
                        "sfs.local-staging.fake-otp.fixed-otp=123456",
                        "sfs.local-staging.fake-otp.phone-numbers[0]=+919900000001"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OtpService.class);
                    assertThat(context).hasSingleBean(FakeOtpService.class);
                    assertThat(context).doesNotHaveBean(TwilioOtpServiceImpl.class);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "staging", "prod"})
    void ordinaryProfilesUseTwilioAndNeverConstructFakeOtpProviderEvenIfPropertyIsSet(String profile) {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=" + profile,
                        // Fixed OTP is disabled outside the local profiles: even if this
                        // property were forced true, FakeOtpService's @Profile excludes
                        // dev/staging/prod, so it can never be constructed here.
                        "sfs.local-staging.fake-otp.enabled=true",
                        "sfs.local-staging.fake-otp.fixed-otp=123456",
                        "sfs.local-staging.fake-otp.phone-numbers[0]=+919900000001"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OtpService.class);
                    assertThat(context).hasSingleBean(TwilioOtpServiceImpl.class);
                    assertThat(context).doesNotHaveBean(FakeOtpService.class);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.profiles.active=local-staging",
            "spring.profiles.active=local-staging,sfs.local-staging.fake-otp.enabled=false"
    })
    void localStagingWithoutFakeOtpExplicitlyEnabledStillBootsWithExactlyOneOtpServiceBean(String rawProperties) {
        // This is the regression this test class exists to guard: before
        // TwilioOtpServiceImpl's @Profile/@ConditionalOnProperty were widened to
        // include local-staging, activating local-staging without also setting
        // SFS_LOCAL_STAGING_FAKE_OTP_ENABLED=true (its documented default is
        // false) produced zero OtpService beans and a startup failure.
        String[] properties = rawProperties.split(",");
        contextRunner.withPropertyValues(properties)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OtpService.class);
                    assertThat(context).hasSingleBean(TwilioOtpServiceImpl.class);
                    assertThat(context).doesNotHaveBean(FakeOtpService.class);
                });
    }

    @Test
    void localStagingWithFakeOtpEnabledConstructsExactlyOneOtpServiceBean() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=local-staging",
                        "sfs.local-staging.fake-otp.enabled=true",
                        "sfs.local-staging.fake-otp.fixed-otp=123456",
                        "sfs.local-staging.fake-otp.phone-numbers[0]=+919900000001"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OtpService.class);
                    assertThat(context).hasSingleBean(FakeOtpService.class);
                    assertThat(context).doesNotHaveBean(TwilioOtpServiceImpl.class);
                });
    }

}
