package com.brandPitara.sfs.ratelimit.config;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;

/**
 * Binds {@code sfs.rate-limit.*} from application.yml. All limits are
 * externally configured here; no limit values are hardcoded in business code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sfs.rate-limit")
@Validated
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
     * to extract narrowly allowed body-derived material (OTP phone and
     * calculator-body fingerprint). Refresh tokens, installation IDs and
     * device IDs are deliberately excluded from cache identity. Requests whose
     * body exceeds this are rejected with 413 before the real controller runs.
     */
    @Min(1024)
    @Max(1024 * 1024)
    private long maxCachedBodyBytes = 32 * 1024;

    /** Capacity multiplier for the independent per-policy IP abuse bucket. */
    @Min(2)
    @Max(100)
    private int abuseCapacityMultiplier = 10;

    /** Policies that return 503 rather than bypassing protection on limiter failure. */
    private Set<RateLimitPolicy> failClosedPolicies = EnumSet.noneOf(RateLimitPolicy.class);

    @Valid
    private BucketCacheProperties bucketCache = new BucketCacheProperties();

    @Valid
    private Map<RateLimitPolicy, PolicyConfig> policies = new EnumMap<>(RateLimitPolicy.class);

    @Getter
    @Setter
    public static class PolicyConfig {
        private Boolean enabled;
        private RateLimitFailureMode failureMode = RateLimitFailureMode.FAIL_OPEN;
        @Valid
        private List<LimitConfig> limits = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class LimitConfig {
        @NotNull
        private RateLimitKeyType keyType;
        @Positive
        private long capacity;
        @Positive
        private long refillTokens;
        @Positive
        private long refillPeriodSeconds;
    }

    /**
     * Independent bounds for principal/body and trusted-IP abuse buckets.
     * Keeping them separate prevents churn in the primary namespace from
     * evicting the IP-level protection.
     */
    @Getter
    @Setter
    public static class BucketCacheProperties {
        @Min(16)
        @Max(100_000)
        private long primaryMaximumSize = 10_000;

        @Min(16)
        @Max(100_000)
        private long abuseMaximumSize = 10_000;

        @Min(1)
        @Max(1440)
        private long expireAfterAccessMinutes = 30;

        @Min(1)
        @Max(1440)
        private long abuseExpireAfterAccessMinutes = 60;

        /** Compatibility alias for older programmatic tests/configuration. */
        public long getMaximumSize() {
            return primaryMaximumSize;
        }

        /** Compatibility alias; applies the same bound to both isolated caches. */
        public void setMaximumSize(long maximumSize) {
            this.primaryMaximumSize = maximumSize;
            this.abuseMaximumSize = maximumSize;
        }
    }
}
