package com.brandPitara.sfs.ratelimit.identity;

/** Request attributes written by JwtRequestFilter after token validation. */
public final class RateLimitAuthenticationAttributes {

    public static final String AUTH_STATE = "sfs.rateLimit.authState";
    public static final String USER_ID = "sfs.rateLimit.userId";
    public static final String GUEST_SESSION_ID = "sfs.rateLimit.guestSessionId";
    public static final String IP_ABUSE_CONSUMED = "sfs.rateLimit.ipAbuseConsumed";

    public static final String STATE_USER = "USER";
    public static final String STATE_GUEST = "GUEST";
    public static final String STATE_INVALID = "INVALID";

    private RateLimitAuthenticationAttributes() {
    }
}
