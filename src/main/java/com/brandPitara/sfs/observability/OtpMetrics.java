package com.brandPitara.sfs.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded-cardinality OTP send/verify metrics. Tags are limited to the fixed
 * set of domain outcome codes below - never phone number, OTP code, userId,
 * or installationId.
 */
@Component
public class OtpMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public OtpMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public static OtpMetrics isolated() {
        return new OtpMetrics(new SimpleMeterRegistry());
    }

    public void sendSuccess() {
        counter("send.success").increment();
    }

    public void sendFailed(String reasonCode) {
        counter("send.failed", "reason", reasonCode).increment();
    }

    public void blocked(String context) {
        counter("blocked", "context", context).increment();
    }

    public void verifySuccess() {
        counter("verify.success").increment();
    }

    public void verifyFailed() {
        counter("verify.failed").increment();
    }

    private Counter counter(String suffix, String... tags) {
        String key = suffix + '|' + String.join("|", tags);
        return counters.computeIfAbsent(key, ignored -> Counter.builder("sfs.otp." + suffix)
                .tags(tags).register(registry));
    }
}
