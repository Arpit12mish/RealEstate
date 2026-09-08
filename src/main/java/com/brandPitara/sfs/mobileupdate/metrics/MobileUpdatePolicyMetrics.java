package com.brandPitara.sfs.mobileupdate.metrics;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MobileUpdatePolicyMetrics {

    private final MeterRegistry registry;

    public MobileUpdatePolicyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordBuildAheadOfPolicy(MobilePlatform platform) {
        Counter.builder("sfs.mobile_update.build_ahead_of_policy")
                .tag("platform", platform.name())
                .register(registry)
                .increment();
    }

    public void recordUnavailablePolicy(MobilePlatform platform, String reason) {
        Counter.builder("sfs.mobile_update.policy_unavailable")
                .tag("platform", platform.name())
                .tag("reason", reason)
                .register(registry)
                .increment();
    }
}
