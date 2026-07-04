package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.LimitConfig;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.PolicyConfig;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.exception.RateLimitConfigurationException;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory Bucket4j implementation of RateLimitService. Suitable for a
 * single application instance (current deployment: one Spring Boot instance
 * on EC2, no shared/distributed state). Buckets live in a Caffeine cache
 * keyed by policy+keyType+identity, bounded by
 * {@code sfs.rate-limit.bucket-cache.maximum-size} and evicted after
 * {@code sfs.rate-limit.bucket-cache.expire-after-access-minutes} of
 * inactivity - so a flood of distinct keys (random phone numbers, search
 * queries, installation ids, IPs, hashed tokens, etc.) cannot grow the
 * process's memory footprint without bound.
 * <p>
 * A future Redis-backed implementation can satisfy {@link RateLimitService}
 * using Bucket4j's distributed ProxyManager without any caller changes.
 */
@Service
@Slf4j
public class InMemoryRateLimitService implements RateLimitService {

    private final RateLimitProperties properties;

    private final Cache<String, Bucket> buckets;

    public InMemoryRateLimitService(RateLimitProperties properties) {
        this.properties = properties;
        RateLimitProperties.BucketCacheProperties cacheConfig = properties.getBucketCache();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getMaximumSize())
                .expireAfterAccess(Duration.ofMinutes(cacheConfig.getExpireAfterAccessMinutes()))
                .build();
    }

    @PostConstruct
    void validateConfiguration() {
        if (!properties.isEnabled()) {
            return;
        }
        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            if (!isPolicyEnabled(policy) ) {
                continue;
            }
            PolicyConfig config = properties.getPolicies().get(policy);
            if (config == null || config.getLimits() == null || config.getLimits().isEmpty()) {
                throw new RateLimitConfigurationException(
                        "sfs.rate-limit is enabled but policy " + policy
                                + " has no configured limits under sfs.rate-limit.policies." + policy + ".limits"
                );
            }
        }
    }

    @Override
    public RateLimitDecision checkAndConsume(RateLimitPolicy policy, Map<RateLimitKeyType, String> resolvedKeys) {
        if (!properties.isEnabled() || !isPolicyEnabled(policy)) {
            return RateLimitDecision.allow(policy);
        }

        PolicyConfig config = properties.getPolicies().get(policy);
        if (config == null || config.getLimits() == null || config.getLimits().isEmpty()) {
            // Unconfigured policy: fail open rather than blocking all traffic on a config gap.
            log.warn("Rate-limit policy {} has no configured limits; allowing request", policy);
            return RateLimitDecision.allow(policy);
        }

        Map<RateLimitKeyType, List<LimitConfig>> limitsByKeyType = config.getLimits().stream()
                .collect(Collectors.groupingBy(LimitConfig::getKeyType));

        // A policy can have several independent dimensions (e.g. PHONE and IP). A
        // request must pass ALL of them, so tokens are only actually spent once every
        // dimension has capacity; any dimension consumed before a later one blocks the
        // request is refunded, so a block on one dimension never silently drains an
        // unrelated bucket (e.g. a blocked phone attempt must not also cost a token
        // against the shared IP bucket that a different phone number relies on).
        List<Bucket> consumedBuckets = new ArrayList<>();

        for (Map.Entry<RateLimitKeyType, List<LimitConfig>> entry : limitsByKeyType.entrySet()) {
            RateLimitKeyType keyType = entry.getKey();
            String rawKey = resolvedKeys.get(keyType);
            if (rawKey == null) {
                // Key material unavailable for this request (e.g. anonymous caller for
                // IP_OR_USER's user dimension); fail open for this dimension only.
                continue;
            }

            String bucketKey = policy.name() + ':' + keyType.name() + ':' + rawKey;
            Bucket bucket = buckets.get(bucketKey, k -> buildBucket(entry.getValue()));

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                consumedBuckets.forEach(b -> b.addTokens(1));
                long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
                return RateLimitDecision.block(policy, keyType, retryAfterSeconds);
            }
            consumedBuckets.add(bucket);
        }

        return RateLimitDecision.allow(policy);
    }

    /** Package-private introspection hook for cache-bound/eviction tests. */
    Cache<String, Bucket> bucketCache() {
        return buckets;
    }

    private boolean isPolicyEnabled(RateLimitPolicy policy) {
        PolicyConfig config = properties.getPolicies().get(policy);
        if (config == null || config.getEnabled() == null) {
            return properties.isDefaultEnabled();
        }
        return config.getEnabled();
    }

    private Bucket buildBucket(List<LimitConfig> limitConfigs) {
        var builder = Bucket.builder();
        for (LimitConfig limit : limitConfigs) {
            Bandwidth bandwidth = Bandwidth.builder()
                    .capacity(limit.getCapacity())
                    .refillIntervally(limit.getRefillTokens(), Duration.ofSeconds(limit.getRefillPeriodSeconds()))
                    .build();
            builder.addLimit(bandwidth);
        }
        return builder.build();
    }
}
