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
    PUBLIC_SEARCH,
    PUBLIC_CITY_READ,
    PUBLIC_BUILDER_READ,
    PUBLIC_BUSINESS_READ,
    PUBLIC_PROVIDER_READ,
    PUBLIC_APP_CONTENT_READ,
    PUBLIC_CALCULATOR_READ,

    // Phase 2: mobile action APIs (still action-based, authenticated,
    // write-heavy, or computationally expensive).
    PUBLIC_CALCULATOR_WRITE,
    MOBILE_PROFILE_READ,
    MOBILE_PROFILE_WRITE,
    MOBILE_FAVORITE_READ,
    MOBILE_FAVORITE_WRITE,
    MOBILE_REVIEW_WRITE,
    MOBILE_REVIEW_READ,
    MOBILE_MEDIA_OR_UPLOAD_ACTION,
    /**
     * Split from the single "MOBILE_PROVIDER_ACCOUNT_ACTION" concept into
     * _READ/_WRITE because each policy binds exactly one limit list, and reads
     * (120/min) and writes (30/min) need different numbers - same convention
     * already used for calculator/profile/favorite above.
     */
    MOBILE_PROVIDER_ACCOUNT_READ,
    MOBILE_PROVIDER_ACCOUNT_WRITE,

    // Phase 3: remaining mobile/public API rate limiting.
    MOBILE_SERVICE_REQUEST_WRITE,
    MOBILE_PROVIDER_INTEREST_WRITE,
    MOBILE_ONBOARDING_WRITE,
    PUBLIC_BUSINESS_EVENT_WRITE,
    PUBLIC_COMPANY_READ,
    PUBLIC_ARCHITECT_DESIGNER_READ,
    PUBLIC_INSTAGRAM_REELS_READ,
    PUBLIC_PROJECT_METER_READ,
    PUBLIC_FEED_READ,

    // Phase 4: final remaining mobile/public read route policies.
    // Note: GET /api/public/cities/trending intentionally reuses PUBLIC_CITY_READ
    // above rather than adding a new value - see RateLimitPolicyResolver.
    PUBLIC_BRAND_READ,
    PUBLIC_DISTRIBUTOR_READ,
    PUBLIC_CATEGORY_READ,
    PUBLIC_CONTENT_VERSION_READ,
    MOBILE_SESSION_READ
}
