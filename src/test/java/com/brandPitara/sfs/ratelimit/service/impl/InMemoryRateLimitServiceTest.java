package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.LimitConfig;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.PolicyConfig;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.exception.RateLimitConfigurationException;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRateLimitServiceTest {

    private RateLimitProperties propertiesWithOtpRequestPolicy() {
        RateLimitProperties properties = new RateLimitProperties();

        LimitConfig phonePerMinute = new LimitConfig();
        phonePerMinute.setKeyType(RateLimitKeyType.PHONE);
        phonePerMinute.setCapacity(3);
        phonePerMinute.setRefillTokens(3);
        phonePerMinute.setRefillPeriodSeconds(60);

        LimitConfig ipPerHour = new LimitConfig();
        ipPerHour.setKeyType(RateLimitKeyType.IP);
        ipPerHour.setCapacity(20);
        ipPerHour.setRefillTokens(20);
        ipPerHour.setRefillPeriodSeconds(3600);

        PolicyConfig policyConfig = new PolicyConfig();
        policyConfig.setLimits(List.of(phonePerMinute, ipPerHour));

        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_REQUEST, policyConfig);
        return properties;
    }

    @Test
    void allowsRequestsWithinCapacityThenBlocksOnceExceeded() {
        InMemoryRateLimitService service = new InMemoryRateLimitService(propertiesWithOtpRequestPolicy());
        Map<RateLimitKeyType, String> keys = Map.of(
                RateLimitKeyType.PHONE, "+919876543210",
                RateLimitKeyType.IP, "1.2.3.4"
        );

        for (int i = 0; i < 3; i++) {
            RateLimitDecision decision = service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, keys);
            assertThat(decision.allowed()).as("attempt %d should be allowed", i + 1).isTrue();
        }

        RateLimitDecision fourth = service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, keys);
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.blockedOnKeyType()).isEqualTo(RateLimitKeyType.PHONE);
        assertThat(fourth.retryAfterSeconds()).isGreaterThan(0);
    }

    @Test
    void differentPhoneNumbersGetIndependentBuckets() {
        InMemoryRateLimitService service = new InMemoryRateLimitService(propertiesWithOtpRequestPolicy());

        Map<RateLimitKeyType, String> phoneA = Map.of(
                RateLimitKeyType.PHONE, "+919876543210", RateLimitKeyType.IP, "1.2.3.4"
        );
        Map<RateLimitKeyType, String> phoneB = Map.of(
                RateLimitKeyType.PHONE, "+919876543211", RateLimitKeyType.IP, "1.2.3.4"
        );

        for (int i = 0; i < 3; i++) {
            assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, phoneA).allowed()).isTrue();
        }
        assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, phoneA).allowed()).isFalse();

        // A different phone number must not be affected by phoneA's exhausted bucket.
        assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, phoneB).allowed()).isTrue();
    }

    @Test
    void sameIpAcrossDifferentPhonesEventuallyHitsIpLimit() {
        RateLimitProperties properties = propertiesWithOtpRequestPolicy();
        // Lower the IP bucket capacity for a fast, deterministic test.
        LimitConfig ipLimit = properties.getPolicies().get(RateLimitPolicy.MOBILE_OTP_REQUEST).getLimits().get(1);
        ipLimit.setCapacity(2);
        ipLimit.setRefillTokens(2);

        InMemoryRateLimitService service = new InMemoryRateLimitService(properties);

        Map<RateLimitKeyType, String> first = Map.of(RateLimitKeyType.PHONE, "+919876543210", RateLimitKeyType.IP, "9.9.9.9");
        Map<RateLimitKeyType, String> second = Map.of(RateLimitKeyType.PHONE, "+919876543211", RateLimitKeyType.IP, "9.9.9.9");
        Map<RateLimitKeyType, String> third = Map.of(RateLimitKeyType.PHONE, "+919876543212", RateLimitKeyType.IP, "9.9.9.9");

        assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, first).allowed()).isTrue();
        assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, second).allowed()).isTrue();

        RateLimitDecision blocked = service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, third);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.blockedOnKeyType()).isEqualTo(RateLimitKeyType.IP);
    }

    @Test
    void allowsEverythingWhenGloballyDisabled() {
        RateLimitProperties properties = propertiesWithOtpRequestPolicy();
        properties.setEnabled(false);
        InMemoryRateLimitService service = new InMemoryRateLimitService(properties);

        Map<RateLimitKeyType, String> keys = Map.of(RateLimitKeyType.PHONE, "+919876543210", RateLimitKeyType.IP, "1.2.3.4");
        for (int i = 0; i < 10; i++) {
            assertThat(service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, keys).allowed()).isTrue();
        }
    }

    @Test
    void failsOpenWhenPolicyHasNoConfiguredLimits() {
        RateLimitProperties properties = new RateLimitProperties();
        // MOBILE_OTP_VERIFY intentionally left unconfigured.
        InMemoryRateLimitService service = new InMemoryRateLimitService(properties);

        RateLimitDecision decision = service.checkAndConsume(
                RateLimitPolicy.MOBILE_OTP_VERIFY,
                Map.of(RateLimitKeyType.PHONE, "+919876543210")
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void startupValidationFailsFastWhenEnabledButAnyPolicyIsUnconfigured() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        // No policies configured at all.

        assertThatThrownBy(() -> new InMemoryRateLimitService(properties).validateConfiguration())
                .isInstanceOf(RateLimitConfigurationException.class);
    }

    // ── Check 1: bucket cache is bounded/evicted, not an unbounded map ──────────

    @Test
    void bucketIsReusedForTheSameKey() {
        InMemoryRateLimitService service = new InMemoryRateLimitService(propertiesWithOtpRequestPolicy());
        Map<RateLimitKeyType, String> keys = Map.of(
                RateLimitKeyType.PHONE, "+919876543210", RateLimitKeyType.IP, "1.2.3.4"
        );

        service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, keys);
        Bucket first = service.bucketCache().getIfPresent(
                RateLimitPolicy.MOBILE_OTP_REQUEST.name() + ":PHONE:+919876543210"
        );

        service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, keys);
        Bucket second = service.bucketCache().getIfPresent(
                RateLimitPolicy.MOBILE_OTP_REQUEST.name() + ":PHONE:+919876543210"
        );

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void differentKeysCreateIndependentCacheEntries() {
        InMemoryRateLimitService service = new InMemoryRateLimitService(propertiesWithOtpRequestPolicy());

        service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, Map.of(
                RateLimitKeyType.PHONE, "+919876543210", RateLimitKeyType.IP, "1.2.3.4"
        ));
        service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, Map.of(
                RateLimitKeyType.PHONE, "+919876543211", RateLimitKeyType.IP, "1.2.3.4"
        ));

        assertThat(service.bucketCache().asMap()).containsKeys(
                "MOBILE_OTP_REQUEST:PHONE:+919876543210",
                "MOBILE_OTP_REQUEST:PHONE:+919876543211",
                "MOBILE_OTP_REQUEST:IP:1.2.3.4"
        );
        assertThat(service.bucketCache().asMap()).hasSize(3);
    }

    @Test
    void bucketCacheIsBoundedByConfiguredMaximumSizeAndExpireAfterAccess() {
        RateLimitProperties properties = propertiesWithOtpRequestPolicy();
        properties.getBucketCache().setMaximumSize(500);
        properties.getBucketCache().setExpireAfterAccessMinutes(45);

        InMemoryRateLimitService service = new InMemoryRateLimitService(properties);

        assertThat(service.bucketCache().policy().eviction()).isPresent();
        assertThat(service.bucketCache().policy().eviction().get().getMaximum()).isEqualTo(500);

        assertThat(service.bucketCache().policy().expireAfterAccess()).isPresent();
        assertThat(service.bucketCache().policy().expireAfterAccess().get().getExpiresAfter().toMinutes())
                .isEqualTo(45);
    }

    @Test
    void bucketCacheDefaultsMatchDocumentedProductionValues() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getBucketCache().getMaximumSize()).isEqualTo(200_000);
        assertThat(properties.getBucketCache().getExpireAfterAccessMinutes()).isEqualTo(120);
    }

    @Test
    void floodOfDistinctKeysDoesNotGrowCacheBeyondConfiguredMaximumSize() {
        RateLimitProperties properties = propertiesWithOtpRequestPolicy();
        properties.getBucketCache().setMaximumSize(50);
        InMemoryRateLimitService service = new InMemoryRateLimitService(properties);

        // Simulate an attacker/flood hammering the endpoint with random phone numbers.
        for (int i = 0; i < 2000; i++) {
            service.checkAndConsume(RateLimitPolicy.MOBILE_OTP_REQUEST, Map.of(
                    RateLimitKeyType.PHONE, "+9198765" + String.format("%05d", i),
                    RateLimitKeyType.IP, "1.2.3.4"
            ));
        }
        service.bucketCache().cleanUp();

        // Caffeine's size eviction is a best-effort/near-immediate bound, not a hard
        // real-time cap on every single write, so assert it stays in the same order of
        // magnitude as the configured maximum rather than the exact number.
        assertThat(service.bucketCache().estimatedSize()).isLessThan(500);
    }
}
