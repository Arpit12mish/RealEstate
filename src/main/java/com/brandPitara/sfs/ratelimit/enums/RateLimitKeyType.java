package com.brandPitara.sfs.ratelimit.enums;

/**
 * Identifies what piece of request identity a limit bucket is keyed on.
 * Values match the {@code keyType} entries used in application.yml.
 */
public enum RateLimitKeyType {
    /** Validated user/guest identity, or a fixed anonymous/invalid-auth IP classification. */
    PRIMARY_IDENTITY,
    /** Canonical trusted client IP, isolated in the abuse-protection cache. */
    IP_ABUSE,
    /** Normalized phone number (via PhoneNumberNormalizer). */
    PHONE,
    /** Legacy configuration alias; active policies use PRIMARY_IDENTITY. */
    IP,
    /** Legacy alias collapsed to PRIMARY_IDENTITY; token material is never used. */
    IP_AND_TOKEN,
    /** Legacy alias collapsed to PRIMARY_IDENTITY; installation IDs are never used. */
    IP_AND_INSTALLATION,
    /** Legacy alias collapsed to PRIMARY_IDENTITY. */
    IP_OR_USER,
    /** Legacy alias collapsed to PRIMARY_IDENTITY; device IDs are never used. */
    IP_AND_DEVICE,
    /** Legacy alias collapsed to PRIMARY_IDENTITY; query text is never used. */
    IP_AND_QUERY,
    /** SHA-256 fingerprint of a canonicalized request body, standalone (not combined with IP). */
    BODY_FINGERPRINT
}
