package com.brandPitara.sfs.ratelimit.model;

import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;

/** Immutable, non-JPA request identity used only for in-process rate limiting. */
public record RateLimitIdentity(
        String primaryKey,
        String abuseIp,
        RateLimitIdentityType type,
        boolean fallback
) {
}
