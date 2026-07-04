package com.brandPitara.sfs.ratelimit.exception;

/**
 * Thrown at startup when rate limiting is enabled but the configuration under
 * {@code sfs.rate-limit.policies} is incomplete for a policy that isn't
 * explicitly disabled. Fails fast instead of silently rate-limiting nothing.
 */
public class RateLimitConfigurationException extends RuntimeException {
    public RateLimitConfigurationException(String message) {
        super(message);
    }
}
