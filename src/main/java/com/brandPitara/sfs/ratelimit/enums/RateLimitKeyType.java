package com.brandPitara.sfs.ratelimit.enums;

/**
 * Identifies what piece of request identity a limit bucket is keyed on.
 * Values match the {@code keyType} entries used in application.yml.
 */
public enum RateLimitKeyType {
    /** Normalized phone number (via PhoneNumberNormalizer). */
    PHONE,
    /** Client IP address (via ClientIpResolver). */
    IP,
    /** Client IP + SHA-256 hash of the refresh token (never the raw token). */
    IP_AND_TOKEN,
    /** Client IP + guest installationId. */
    IP_AND_INSTALLATION,
    /** Authenticated userId if present, otherwise falls back to client IP. */
    IP_OR_USER,
    /** Client IP + deviceId (falls back to IP-only when deviceId is absent). */
    IP_AND_DEVICE,
    /** Client IP + a normalized fingerprint of the search query string. */
    IP_AND_QUERY,
    /** SHA-256 fingerprint of a canonicalized request body, standalone (not combined with IP). */
    BODY_FINGERPRINT
}
