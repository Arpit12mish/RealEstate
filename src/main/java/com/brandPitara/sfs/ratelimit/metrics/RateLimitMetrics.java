package com.brandPitara.sfs.ratelimit.metrics;

import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Aggregate metrics with bounded enum/category tags only. */
@Component
public class RateLimitMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public static RateLimitMetrics isolated() {
        return new RateLimitMetrics(new SimpleMeterRegistry());
    }

    public void bindCache(String cacheName, Cache<String, Bucket> cache) {
        Gauge.builder("sfs.rate_limit.cache.size", cache, value -> value.estimatedSize())
                .tag("cache", cacheName).register(registry);
        FunctionCounter.builder("sfs.rate_limit.cache.hits", cache,
                        value -> value.stats().hitCount())
                .tag("cache", cacheName).register(registry);
        FunctionCounter.builder("sfs.rate_limit.cache.misses", cache,
                        value -> value.stats().missCount())
                .tag("cache", cacheName).register(registry);
        FunctionCounter.builder("sfs.rate_limit.cache.evictions", cache,
                        value -> value.stats().evictionCount())
                .tag("cache", cacheName).register(registry);
    }

    public void bucketCreated(String cacheName) {
        counter("bucket.created", "cache", cacheName).increment();
    }

    public void decision(RateLimitPolicy policy, RateLimitIdentityType identityType, boolean allowed) {
        counter(allowed ? "allowed" : "rejected",
                "policy", policy.name(), "identity", identityType.name()).increment();
    }

    public void identityFallback(RateLimitIdentityType identityType) {
        counter("identity.fallback", "identity", identityType.name()).increment();
    }

    public void invalidAuthenticationRejected(RateLimitPolicy policy) {
        counter("invalid_auth.rejected", "policy", policy.name()).increment();
    }

    public void payloadRejected(RateLimitPolicy policy) {
        counter("payload.rejected", "policy", policy.name()).increment();
    }

    public void enforcementFailure(RateLimitPolicy policy, RateLimitFailureMode mode) {
        counter("failure", "policy", policy.name(), "mode", mode.name()).increment();
    }

    MeterRegistry registry() {
        return registry;
    }

    private Counter counter(String suffix, String... tags) {
        String key = suffix + '|' + String.join("|", tags);
        return counters.computeIfAbsent(key, ignored -> Counter.builder("sfs.rate_limit." + suffix)
                .tags(tags).register(registry));
    }
}
