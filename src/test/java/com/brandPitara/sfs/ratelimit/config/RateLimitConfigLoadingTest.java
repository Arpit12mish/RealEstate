package com.brandPitara.sfs.ratelimit.config;

import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves RateLimitProperties actually binds from the tracked
 * src/main/resources/application-rate-limit.yml file via real Spring Boot
 * config-data loading (spring.config.import) - not just from a manually
 * constructed RateLimitProperties object in a unit test.
 * <p>
 * application.yml (which used to hold this config inline) is gitignored
 * because it carries real secrets, so it must never be the only place Phase 1
 * / Phase 1.5 policy config lives. This test deliberately does NOT rely on
 * application.yml being present at all: it imports application-rate-limit.yml
 * directly, the same way application-prod.yml and application-test.yml do.
 */
@SpringBootTest(
        classes = RateLimitConfigLoadingTest.TestApplication.class,
        properties = "spring.config.import=classpath:application-rate-limit.yml"
)
class RateLimitConfigLoadingTest {

    @Autowired
    private RateLimitProperties properties;

    @Test
    void bindsMasterSwitchesFromTrackedConfigFile() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isDefaultEnabled()).isTrue();
        assertThat(properties.getTrustedProxies()).contains("127.0.0.1", "::1");
        assertThat(properties.getMaxCachedBodyBytes()).isEqualTo(32 * 1024);
        assertThat(properties.getBucketCache().getMaximumSize()).isEqualTo(200_000);
        assertThat(properties.getBucketCache().getExpireAfterAccessMinutes()).isEqualTo(120);
    }

    @Test
    void bindsEveryPhase1Policy() {
        assertThat(properties.getPolicies()).containsKeys(
                RateLimitPolicy.MOBILE_OTP_REQUEST,
                RateLimitPolicy.MOBILE_OTP_VERIFY,
                RateLimitPolicy.MOBILE_TOKEN_REFRESH,
                RateLimitPolicy.MOBILE_LOGOUT,
                RateLimitPolicy.MOBILE_LOGOUT_ALL,
                RateLimitPolicy.MOBILE_GUEST_SESSION,
                RateLimitPolicy.PUBLIC_HOME_READ,
                RateLimitPolicy.PUBLIC_PROJECT_READ,
                RateLimitPolicy.PUBLIC_PROJECT_COMPARE,
                RateLimitPolicy.PUBLIC_LOCATION_RESOLVE,
                RateLimitPolicy.PUBLIC_SEARCH
        );
    }

    @Test
    void bindsEveryPhase1_5Policy() {
        assertThat(properties.getPolicies()).containsKeys(
                RateLimitPolicy.PUBLIC_CITY_READ,
                RateLimitPolicy.PUBLIC_BUILDER_READ,
                RateLimitPolicy.PUBLIC_BUSINESS_READ,
                RateLimitPolicy.PUBLIC_PROVIDER_READ,
                RateLimitPolicy.PUBLIC_APP_CONTENT_READ,
                RateLimitPolicy.PUBLIC_CALCULATOR_READ
        );
    }

    @Test
    void bindsFullLimitDetailForAMultiDimensionPolicy() {
        RateLimitProperties.PolicyConfig otpRequest = properties.getPolicies().get(RateLimitPolicy.MOBILE_OTP_REQUEST);

        assertThat(otpRequest.getLimits()).hasSize(3);
        assertThat(otpRequest.getLimits().get(0).getCapacity()).isEqualTo(3);
        assertThat(otpRequest.getLimits().get(0).getRefillPeriodSeconds()).isEqualTo(60);
    }

    @Test
    void bindsFullLimitDetailForANewPhase1_5Policy() {
        RateLimitProperties.PolicyConfig calculatorRead =
                properties.getPolicies().get(RateLimitPolicy.PUBLIC_CALCULATOR_READ);

        assertThat(calculatorRead.getLimits()).hasSize(1);
        assertThat(calculatorRead.getLimits().get(0).getCapacity()).isEqualTo(60);
    }

    // ── Regression guard: the profile-specific files must keep declaring the import ──

    @Test
    void prodProfileDeclaresTheRateLimitConfigImport() throws IOException {
        assertThat(readClasspathResource("application-prod.yml"))
                .contains("application-rate-limit.yml");
    }

    @Test
    void testProfileDeclaresTheRateLimitConfigImport() throws IOException {
        assertThat(readClasspathResource("application-test.yml"))
                .contains("application-rate-limit.yml");
    }

    private String readClasspathResource(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource(name);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    @SpringBootConfiguration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class TestApplication {
    }
}
