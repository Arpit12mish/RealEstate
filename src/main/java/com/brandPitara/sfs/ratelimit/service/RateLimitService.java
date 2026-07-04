package com.brandPitara.sfs.ratelimit.service;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;

import java.util.Map;

/**
 * Checks-and-consumes rate-limit tokens for a policy. The in-memory
 * implementation backs this with local Bucket4j buckets; a future
 * Redis-backed implementation can satisfy the same interface (e.g. via
 * Bucket4j's distributed ProxyManager) without callers changing.
 */
public interface RateLimitService {

    /**
     * @param policy the policy being enforced
     * @param resolvedKeys raw key material already resolved for each key type
     *                     this policy's configured limits reference (see
     *                     RateLimitKeyResolver); a key type with no configured
     *                     limit for this policy is ignored
     */
    RateLimitDecision checkAndConsume(RateLimitPolicy policy, Map<RateLimitKeyType, String> resolvedKeys);
}
