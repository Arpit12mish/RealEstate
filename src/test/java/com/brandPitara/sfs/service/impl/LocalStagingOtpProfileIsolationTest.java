package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStagingOtpProfileIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(LogSanitizer.class)
            .withUserConfiguration(FakeOtpService.class);

    @Test
    void localStagingRequiresExplicitFakeOtpEnablement() {
        contextRunner.withPropertyValues("spring.profiles.active=local-staging")
                .run(context -> assertThat(context).doesNotHaveBean(OtpService.class));

        contextRunner.withPropertyValues(
                        "spring.profiles.active=local-staging",
                        "sfs.local-staging.fake-otp.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OtpService.class);
                    assertThat(context).hasSingleBean(FakeOtpService.class);
                });
    }

    @Test
    void productionCannotActivateFixedOtpEvenWhenPropertyIsSet() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "sfs.local-staging.fake-otp.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(FakeOtpService.class));
    }
}
