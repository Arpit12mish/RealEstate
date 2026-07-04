package com.brandPitara.sfs.ratelimit.enums;

/**
 * Named rate-limit policies. Each value corresponds to a key under
 * {@code sfs.rate-limit.policies} in application.yml, and to a route
 * mapping in RateLimitPolicyResolver.
 */
public enum RateLimitPolicy {
    MOBILE_OTP_REQUEST,
    MOBILE_OTP_VERIFY,
    MOBILE_TOKEN_REFRESH,
    MOBILE_LOGOUT,
    MOBILE_LOGOUT_ALL,
    MOBILE_GUEST_SESSION,
    PUBLIC_HOME_READ,
    PUBLIC_PROJECT_READ,
    PUBLIC_PROJECT_COMPARE,
    PUBLIC_LOCATION_RESOLVE,
    PUBLIC_SEARCH
}
