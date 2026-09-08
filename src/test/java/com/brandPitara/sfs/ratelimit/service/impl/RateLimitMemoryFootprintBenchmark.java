package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in retained-heap benchmark; excluded from the normal *Test naming pattern. */
class RateLimitMemoryFootprintBenchmark {

    @Test
    void measureRetainedHeapAtRepresentativeEntryCounts() throws Exception {
        forceGc();
        long baseline = usedHeap();
        InMemoryRateLimitService service = service(100_000, 16);
        int previous = 0;
        System.out.printf("rate-limit-footprint entries=0 retainedBytes=0 bytesPerEntry=0.0%n");
        for (int target : new int[]{1_000, 10_000, 50_000, 100_000}) {
            long started = System.nanoTime();
            for (int index = previous; index < target; index++) {
                service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ,
                        Map.of(RateLimitKeyType.PRIMARY_IDENTITY, "user:" + index));
            }
            long elapsed = System.nanoTime() - started;
            forceGc();
            long retained = Math.max(0, usedHeap() - baseline);
            System.out.printf("rate-limit-footprint entries=%d retainedBytes=%d bytesPerEntry=%.1f creationNsPerEntry=%.1f%n",
                    target, retained, retained / (double) target, elapsed / (double) (target - previous));
            previous = target;
        }
        assertThat(service.bucketCache().estimatedSize()).isLessThanOrEqualTo(100_000);
    }

    @Test
    void measureFinalTwentyThousandEntryBound() throws Exception {
        forceGc();
        long baseline = usedHeap();
        InMemoryRateLimitService service = service(10_000, 10_000);
        long started = System.nanoTime();
        for (int index = 0; index < 10_000; index++) {
            service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ, Map.of(
                    RateLimitKeyType.PRIMARY_IDENTITY, "user:" + index,
                    RateLimitKeyType.IP_ABUSE, "2001:db8::" + Integer.toHexString(index)));
        }
        long elapsed = System.nanoTime() - started;
        forceGc();
        long retained = Math.max(0, usedHeap() - baseline);
        System.out.printf("rate-limit-final entries=%d retainedBytes=%d bytesPerEntry=%.1f creationNsPerRequest=%.1f%n",
                service.bucketCache().estimatedSize() + service.abuseBucketCache().estimatedSize(),
                retained, retained / 20_000.0, elapsed / 10_000.0);
        assertThat(service.bucketCache().estimatedSize()).isLessThanOrEqualTo(10_000);
        assertThat(service.abuseBucketCache().estimatedSize()).isLessThanOrEqualTo(10_000);
    }

    private InMemoryRateLimitService service(long primaryMaximum, long abuseMaximum) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getBucketCache().setPrimaryMaximumSize(primaryMaximum);
        properties.getBucketCache().setAbuseMaximumSize(abuseMaximum);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setLimits(List.of(limit(RateLimitKeyType.PRIMARY_IDENTITY), limit(RateLimitKeyType.IP_ABUSE)));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy);
        return new InMemoryRateLimitService(properties);
    }

    private RateLimitProperties.LimitConfig limit(RateLimitKeyType type) {
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(type);
        limit.setCapacity(100_000);
        limit.setRefillTokens(100_000);
        limit.setRefillPeriodSeconds(60);
        return limit;
    }

    private void forceGc() throws InterruptedException {
        System.gc();
        Thread.sleep(100);
    }

    private long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
