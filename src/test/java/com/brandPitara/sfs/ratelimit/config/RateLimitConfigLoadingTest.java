package com.brandPitara.sfs.ratelimit.config;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
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
    void bindsEveryPhase2Policy() {
        assertThat(properties.getPolicies()).containsKeys(
                RateLimitPolicy.PUBLIC_CALCULATOR_WRITE,
                RateLimitPolicy.MOBILE_PROFILE_READ,
                RateLimitPolicy.MOBILE_PROFILE_WRITE,
                RateLimitPolicy.MOBILE_FAVORITE_READ,
                RateLimitPolicy.MOBILE_FAVORITE_WRITE,
                RateLimitPolicy.MOBILE_REVIEW_WRITE,
                RateLimitPolicy.MOBILE_REVIEW_READ,
                RateLimitPolicy.MOBILE_MEDIA_OR_UPLOAD_ACTION,
                RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ,
                RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE
        );
    }

    @Test
    void bindsFullLimitDetailForANewPhase2BodyFingerprintPolicy() {
        RateLimitProperties.PolicyConfig calculatorWrite =
                properties.getPolicies().get(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE);

        assertThat(calculatorWrite.getLimits()).hasSize(3);
        assertThat(calculatorWrite.getLimits())
                .extracting(RateLimitProperties.LimitConfig::getKeyType)
                .contains(RateLimitKeyType.IP, RateLimitKeyType.BODY_FINGERPRINT);
    }

    @Test
    void bindsMultiWindowLimitsForAPhase2ReviewWritePolicy() {
        RateLimitProperties.PolicyConfig reviewWrite =
                properties.getPolicies().get(RateLimitPolicy.MOBILE_REVIEW_WRITE);

        assertThat(reviewWrite.getLimits()).hasSize(2);
        assertThat(reviewWrite.getLimits().get(0).getCapacity()).isEqualTo(5);
        assertThat(reviewWrite.getLimits().get(1).getRefillPeriodSeconds()).isEqualTo(86400);
    }

    @Test
    void bindsEveryPhase3Policy() {
        assertThat(properties.getPolicies()).containsKeys(
                RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE,
                RateLimitPolicy.MOBILE_PROVIDER_INTEREST_WRITE,
                RateLimitPolicy.MOBILE_ONBOARDING_WRITE,
                RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE,
                RateLimitPolicy.PUBLIC_COMPANY_READ,
                RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ,
                RateLimitPolicy.PUBLIC_INSTAGRAM_REELS_READ,
                RateLimitPolicy.PUBLIC_PROJECT_METER_READ,
                RateLimitPolicy.PUBLIC_FEED_READ
        );
    }

    @Test
    void bindsMultiWindowLimitsForAPhase3ServiceRequestWritePolicy() {
        RateLimitProperties.PolicyConfig serviceRequestWrite =
                properties.getPolicies().get(RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE);

        assertThat(serviceRequestWrite.getLimits()).hasSize(2);
        assertThat(serviceRequestWrite.getLimits().get(0).getCapacity()).isEqualTo(20);
        assertThat(serviceRequestWrite.getLimits().get(1).getRefillPeriodSeconds()).isEqualTo(3600);
    }

    @Test
    void bindsIpKeyedLimitsForAPhase3PublicWritePolicy() {
        RateLimitProperties.PolicyConfig businessEventWrite =
                properties.getPolicies().get(RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE);

        assertThat(businessEventWrite.getLimits()).hasSize(2);
        assertThat(businessEventWrite.getLimits())
                .extracting(RateLimitProperties.LimitConfig::getKeyType)
                .containsOnly(RateLimitKeyType.IP);
    }

    @Test
    void bindsEveryPhase4Policy() {
        assertThat(properties.getPolicies()).containsKeys(
                RateLimitPolicy.PUBLIC_BRAND_READ,
                RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ,
                RateLimitPolicy.PUBLIC_CATEGORY_READ,
                RateLimitPolicy.PUBLIC_CONTENT_VERSION_READ,
                RateLimitPolicy.MOBILE_SESSION_READ
        );
    }

    @Test
    void trendingCityRouteReusesPublicCityReadWithoutADuplicateConfigEntry() {
        // GET /api/public/cities/trending is mapped (in RateLimitPolicyResolver) to
        // the existing PUBLIC_CITY_READ policy rather than a new one - there must be
        // exactly one config entry for it, already asserted by bindsEveryPhase1Policy.
        assertThat(properties.getPolicies().get(RateLimitPolicy.PUBLIC_CITY_READ)).isNotNull();
        assertThat(properties.getPolicies().get(RateLimitPolicy.PUBLIC_CITY_READ).getLimits()).hasSize(1);
    }

    @Test
    void bindsAGenerousLimitForTheContentVersionReadPolicy() {
        RateLimitProperties.PolicyConfig contentVersionRead =
                properties.getPolicies().get(RateLimitPolicy.PUBLIC_CONTENT_VERSION_READ);

        assertThat(contentVersionRead.getLimits()).hasSize(1);
        assertThat(contentVersionRead.getLimits().get(0).getKeyType()).isEqualTo(RateLimitKeyType.IP);
        assertThat(contentVersionRead.getLimits().get(0).getCapacity()).isEqualTo(600);
    }

    @Test
    void bindsUserPreferredKeyForTheSessionReadPolicy() {
        RateLimitProperties.PolicyConfig sessionRead =
                properties.getPolicies().get(RateLimitPolicy.MOBILE_SESSION_READ);

        assertThat(sessionRead.getLimits()).hasSize(1);
        assertThat(sessionRead.getLimits().get(0).getKeyType()).isEqualTo(RateLimitKeyType.IP_OR_USER);
        assertThat(sessionRead.getLimits().get(0).getCapacity()).isEqualTo(300);
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
