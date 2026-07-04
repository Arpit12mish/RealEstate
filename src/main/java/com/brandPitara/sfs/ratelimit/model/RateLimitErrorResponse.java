package com.brandPitara.sfs.ratelimit.model;

import lombok.Builder;
import lombok.Getter;

/**
 * JSON body written directly to the response by RateLimitingFilter when a
 * request is blocked. Field order matches the declared/agreed API shape.
 */
@Getter
@Builder
public class RateLimitErrorResponse {
    private final int status;
    private final String error;
    private final String message;
    private final long retryAfterSeconds;
    private final String policy;
}
