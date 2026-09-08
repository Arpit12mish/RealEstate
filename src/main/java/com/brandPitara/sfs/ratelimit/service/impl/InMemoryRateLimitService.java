package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.LimitConfig;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.PolicyConfig;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.exception.RateLimitConfigurationException;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory Bucket4j implementation of RateLimitService. Suitable for a
 * single application instance (current deployment: one Spring Boot instance
 * on EC2, no shared/distributed state). Primary/supplemental dimensions and
 * canonical IP-abuse dimensions use separate bounded Caffeine caches so
 * attacker-controlled content churn cannot evict IP protection.
 * <p>
 * A future Redis-backed implementation can satisfy {@link RateLimitService}
 * using Bucket4j's distributed ProxyManager without any caller changes.
 */
@Service
@Slf4j
public class InMemoryRateLimitService implements RateLimitService {

    private final RateLimitProperties properties;

    private final Cache<String, Bucket> primaryBuckets;
    private final Cache<String, Bucket> abuseBuckets;
    private final RateLimitMetrics metrics;

    public InMemoryRateLimitService(RateLimitProperties properties) {
        this(properties, RateLimitMetrics.isolated());
    }

    @Autowired
    public InMemoryRateLimitService(RateLimitProperties properties, RateLimitMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        RateLimitProperties.BucketCacheProperties cacheConfig = properties.getBucketCache();
        this.primaryBuckets = buildCache(
                cacheConfig.getPrimaryMaximumSize(),
                cacheConfig.getExpireAfterAccessMinutes()
        );
        this.abuseBuckets = buildCache(
                cacheConfig.getAbuseMaximumSize(),
                cacheConfig.getAbuseExpireAfterAccessMinutes()
        );
        metrics.bindCache("primary", primaryBuckets);
        metrics.bindCache("abuse", abuseBuckets);
    }

    private Cache<String, Bucket> buildCache(long maximumSize, long expiryMinutes) {
        return Caffeine.<String, Bucket>newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(Duration.ofMinutes(expiryMinutes))
                .recordStats()
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
            throw new RateLimitConfigurationException("Enabled policy " + policy + " has no limits");
        }

        Map<RateLimitKeyType, List<LimitConfig>> limitsByKeyType = config.getLimits().stream()
                .collect(Collectors.groupingBy(LimitConfig::getKeyType));
        if (resolvedKeys.containsKey(RateLimitKeyType.IP_ABUSE)) {
            limitsByKeyType.computeIfAbsent(RateLimitKeyType.IP_ABUSE,
                    ignored -> scaledAbuseLimits(config.getLimits()));
        } else {
            limitsByKeyType.remove(RateLimitKeyType.IP_ABUSE);
        }

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
            if (rawKey == null || rawKey.isBlank()) {
                // This dimension could not be resolved for this request (e.g. PHONE
                // on a request with no/malformed phone number) - skip just this
                // dimension rather than failing the whole multi-dimension check.
                // RateLimitKeyResolver's contract is to omit an unresolvable
                // dimension, not to fail the request; every other configured
                // dimension (e.g. PRIMARY_IDENTITY, IP) still applies below.
                log.debug(
                        "Skipping unresolved rate-limit dimension: policy={} keyType={}",
                        policy, keyType
                );
                continue;
            }

            String bucketKey = policy.name() + ':' + keyType.name() + ':' + rawKey;
            boolean abuseDimension = keyType == RateLimitKeyType.IP_ABUSE;
            Cache<String, Bucket> cache = abuseDimension ? abuseBuckets : primaryBuckets;
            String cacheName = abuseDimension ? "abuse" : "primary";
            Bucket bucket = cache.get(bucketKey, k -> {
                metrics.bucketCreated(cacheName);
                return buildBucket(entry.getValue());
            });

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                consumedBuckets.forEach(b -> b.addTokens(1));
                long retryAfterSeconds = ceilSeconds(probe.getNanosToWaitForRefill());
                return RateLimitDecision.block(policy, keyType, retryAfterSeconds);
            }
            consumedBuckets.add(bucket);
        }

        return RateLimitDecision.allow(policy);
    }

    @Override
    public RateLimitDecision checkAndConsumeAbuse(RateLimitPolicy policy, String canonicalClientIp) {
        if (!properties.isEnabled() || !isPolicyEnabled(policy)) {
            return RateLimitDecision.allow(policy);
        }
        if (canonicalClientIp == null || canonicalClientIp.isBlank()) {
            throw new RateLimitConfigurationException("Canonical client IP is required for " + policy);
        }
        PolicyConfig config = properties.getPolicies().get(policy);
        if (config == null || config.getLimits() == null || config.getLimits().isEmpty()) {
            throw new RateLimitConfigurationException("Enabled policy " + policy + " has no limits");
        }
        String bucketKey = policy.name() + ":IP_ABUSE:" + canonicalClientIp;
        Bucket bucket = abuseBuckets.get(bucketKey, ignored -> {
            metrics.bucketCreated("abuse");
            return buildBucket(scaledAbuseLimits(config.getLimits()));
        });
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return probe.isConsumed()
                ? RateLimitDecision.allow(policy)
                : RateLimitDecision.block(policy, RateLimitKeyType.IP_ABUSE,
                        ceilSeconds(probe.getNanosToWaitForRefill()));
    }

    private long ceilSeconds(long nanos) {
        if (nanos <= 0) return 1;
        long seconds = nanos / 1_000_000_000L;
        return Math.max(1, seconds + (nanos % 1_000_000_000L == 0 ? 0 : 1));
    }

    /** Package-private introspection hook for cache-bound/eviction tests. */
    Cache<String, Bucket> bucketCache() {
        return primaryBuckets;
    }

    /** Package-private introspection for isolated IP-abuse eviction tests. */
    Cache<String, Bucket> abuseBucketCache() {
        return abuseBuckets;
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

    private List<LimitConfig> scaledAbuseLimits(List<LimitConfig> configuredLimits) {
        int multiplier = properties.getAbuseCapacityMultiplier();
        return configuredLimits.stream().map(limit -> {
            LimitConfig abuse = new LimitConfig();
            abuse.setKeyType(RateLimitKeyType.IP_ABUSE);
            abuse.setCapacity(saturatedMultiply(limit.getCapacity(), multiplier));
            abuse.setRefillTokens(saturatedMultiply(limit.getRefillTokens(), multiplier));
            abuse.setRefillPeriodSeconds(limit.getRefillPeriodSeconds());
            return abuse;
        }).toList();
    }

    private long saturatedMultiply(long value, int multiplier) {
        if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE;
        return value * multiplier;
    }
}
