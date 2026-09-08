package com.brandPitara.sfs.ratelimit.enums;

/** Behavior when rate-limit identity resolution or enforcement itself fails. */
public enum RateLimitFailureMode {
    FAIL_OPEN,
    FAIL_CLOSED
}
