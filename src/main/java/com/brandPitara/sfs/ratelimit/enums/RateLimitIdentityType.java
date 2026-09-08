package com.brandPitara.sfs.ratelimit.enums;

/** Low-cardinality authentication classification used by rate-limit metrics. */
public enum RateLimitIdentityType {
    AUTHENTICATED_USER,
    VALID_GUEST,
    ANONYMOUS,
    INVALID_AUTH
}
