package com.brandPitara.sfs.ratelimit.model;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;

/**
 * Outcome of a rate-limit check: whether the request is allowed, and if not,
 * how long the caller should wait before retrying.
 */
public record RateLimitDecision(
        boolean allowed,
        RateLimitPolicy policy,
        RateLimitKeyType blockedOnKeyType,
        long retryAfterSeconds
) {

    public static RateLimitDecision allow(RateLimitPolicy policy) {
        return new RateLimitDecision(true, policy, null, 0);
    }

    public static RateLimitDecision block(RateLimitPolicy policy, RateLimitKeyType keyType, long retryAfterSeconds) {
        return new RateLimitDecision(false, policy, keyType, Math.max(1, retryAfterSeconds));
    }
}
