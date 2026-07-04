package com.brandPitara.sfs.ratelimit.config;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Binds {@code sfs.rate-limit.*} from application.yml. All limits are
 * externally configured here; no limit values are hardcoded in business code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sfs.rate-limit")
public class RateLimitProperties {

    /** Master switch. When false, the filter allows every request unconditionally. */
    private boolean enabled = true;

    /**
     * Default enabled state for a policy that has no explicit {@code enabled} flag
     * set under {@code policies.<POLICY>.enabled}.
     */
    private boolean defaultEnabled = true;

    /**
     * Trusted proxy addresses. X-Forwarded-For is only honored when the direct
     * TCP peer (request.getRemoteAddr()) is one of these. Defaults to loopback,
     * matching a single-EC2-instance deployment where nginx runs locally in
     * front of the app.
     */
    private List<String> trustedProxies = new ArrayList<>(List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));

    /**
     * Maximum size, in bytes, of a request body RateLimitingFilter will buffer
     * to extract key material (phone number, refresh token, installationId,
     * deviceId) for the body-based policies. Requests whose body exceeds this
     * are rejected with 413 before the real controller ever runs.
     */
    private long maxCachedBodyBytes = 32 * 1024;

    private BucketCacheProperties bucketCache = new BucketCacheProperties();

    private Map<RateLimitPolicy, PolicyConfig> policies = new EnumMap<>(RateLimitPolicy.class);

    @Getter
    @Setter
    public static class PolicyConfig {
        private Boolean enabled;
        private List<LimitConfig> limits = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class LimitConfig {
        private RateLimitKeyType keyType;
        private long capacity;
        private long refillTokens;
        private long refillPeriodSeconds;
    }

    /**
     * Bounds for the local Bucket4j bucket cache, so distinct rate-limit keys
     * (random phone numbers, search queries, installation ids, IPs, hashed
     * tokens, etc.) cannot grow the process's memory footprint without limit.
     */
    @Getter
    @Setter
    public static class BucketCacheProperties {
        private long maximumSize = 200_000;
        private long expireAfterAccessMinutes = 120;
    }
}
