package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitCacheHardeningTest {

    @Test
    void concurrentFirstRequestCreatesExactlyOneBucketAtomically() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryRateLimitService service = service(1_000, 1_000, registry,
                limit(RateLimitKeyType.PRIMARY_IDENTITY, 1_000));
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        for (int index = 0; index < 100; index++) {
            executor.submit(() -> {
                start.await();
                if (service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                        Map.of(RateLimitKeyType.PRIMARY_IDENTITY, "user:42")).allowed()) {
                    allowed.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(allowed).hasValue(100);
        assertThat(service.bucketCache().estimatedSize()).isEqualTo(1);
        assertThat(registry.get("sfs.rate_limit.bucket.created").tag("cache", "primary").counter().count())
                .isEqualTo(1);
    }

    @Test
    void cacheMaximumAndEvictionMetricAreVisible() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryRateLimitService service = service(16, 16, registry,
                limit(RateLimitKeyType.PRIMARY_IDENTITY, 1_000));
        for (int index = 0; index < 2_000; index++) {
            service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                    Map.of(RateLimitKeyType.PRIMARY_IDENTITY, "user:" + index));
        }
        service.bucketCache().cleanUp();

        assertThat(service.bucketCache().estimatedSize()).isLessThanOrEqualTo(16);
        assertThat(registry.get("sfs.rate_limit.cache.evictions").tag("cache", "primary")
                .functionCounter().count()).isGreaterThan(0);
    }

    @Test
    void expiryCleanupRemovesInactiveBuckets() throws Exception {
        InMemoryRateLimitService service = service(16, 16, new SimpleMeterRegistry(),
                limit(RateLimitKeyType.PRIMARY_IDENTITY, 100));
        service.bucketCache().policy().expireAfterAccess().orElseThrow()
                .setExpiresAfter(Duration.ofNanos(1));
        service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                Map.of(RateLimitKeyType.PRIMARY_IDENTITY, "user:1"));
        Thread.sleep(2);
        service.bucketCache().cleanUp();

        assertThat(service.bucketCache().estimatedSize()).isZero();
    }

    @Test
    void thousandsOfInvalidRefreshValuesUseOneFixedIpScopedBucket() {
        InMemoryRateLimitService service = service(100, 100, new SimpleMeterRegistry(),
                limit(RateLimitKeyType.IP_AND_TOKEN, 20_000));
        String fixedInvalidIdentity = "invalid-auth:203.0.113.44|_none_";
        for (int index = 0; index < 10_000; index++) {
            service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                    Map.of(RateLimitKeyType.IP_AND_TOKEN, fixedInvalidIdentity));
        }

        assertThat(service.bucketCache().estimatedSize()).isEqualTo(1);
        assertThat(service.bucketCache().asMap().keySet())
                .noneMatch(key -> key.contains("invalid-refresh-token"));
    }

    @Test
    void primaryCacheChurnCannotEvictOrBypassIpAbuseBucket() {
        RateLimitProperties.LimitConfig primary = limit(RateLimitKeyType.BODY_FINGERPRINT, 10_000);
        RateLimitProperties.LimitConfig abuse = limit(RateLimitKeyType.IP_ABUSE, 3);
        InMemoryRateLimitService service = service(16, 16, new SimpleMeterRegistry(), primary, abuse);

        for (int index = 0; index < 2_000; index++) {
            service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                    Map.of(RateLimitKeyType.BODY_FINGERPRINT, "body:" + index));
        }
        for (int index = 0; index < 3; index++) {
            assertThat(service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ, Map.of(
                    RateLimitKeyType.BODY_FINGERPRINT, "probe:" + index,
                    RateLimitKeyType.IP_ABUSE, "203.0.113.50")).allowed()).isTrue();
        }
        assertThat(service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ, Map.of(
                RateLimitKeyType.BODY_FINGERPRINT, "probe:blocked",
                RateLimitKeyType.IP_ABUSE, "203.0.113.50")).allowed()).isFalse();
        assertThat(service.abuseBucketCache().estimatedSize()).isEqualTo(1);
    }

    private InMemoryRateLimitService service(long primaryMaximum, long abuseMaximum,
                                             SimpleMeterRegistry registry,
                                             RateLimitProperties.LimitConfig... limits) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getBucketCache().setPrimaryMaximumSize(primaryMaximum);
        properties.getBucketCache().setAbuseMaximumSize(abuseMaximum);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setLimits(List.of(limits));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy);
        return new InMemoryRateLimitService(properties, new RateLimitMetrics(registry));
    }

    private RateLimitProperties.LimitConfig limit(RateLimitKeyType type, long capacity) {
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(type);
        limit.setCapacity(capacity);
        limit.setRefillTokens(capacity);
        limit.setRefillPeriodSeconds(60);
        return limit;
    }
}
