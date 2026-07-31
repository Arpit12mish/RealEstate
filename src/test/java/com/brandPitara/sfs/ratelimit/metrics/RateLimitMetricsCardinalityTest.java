package com.brandPitara.sfs.ratelimit.metrics;

import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitMetricsCardinalityTest {

    @Test
    void metricTagsAreRestrictedToBoundedEnumerations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetrics metrics = new RateLimitMetrics(registry);
        metrics.decision(RateLimitPolicy.PUBLIC_HOME_READ, RateLimitIdentityType.AUTHENTICATED_USER, true);
        metrics.decision(RateLimitPolicy.PUBLIC_HOME_READ, RateLimitIdentityType.INVALID_AUTH, false);
        metrics.identityFallback(RateLimitIdentityType.ANONYMOUS);
        metrics.invalidAuthenticationRejected(RateLimitPolicy.PUBLIC_HOME_READ);
        metrics.enforcementFailure(RateLimitPolicy.PUBLIC_HOME_READ, RateLimitFailureMode.FAIL_OPEN);

        Set<String> permitted = Set.of("cache", "policy", "identity", "mode");
        assertThat(registry.getMeters()).allSatisfy(meter -> {
            assertThat(meter.getId().getTags()).allSatisfy(tag -> {
                assertThat(permitted).contains(tag.getKey());
                assertThat(tag.getValue()).doesNotContain("203.0.113", "user:", "guest:", "Bearer", "token");
            });
        });
    }
}
