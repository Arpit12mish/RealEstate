package com.brandPitara.sfs.ratelimit.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Raw (pre-key-building) identity material extracted from a single request:
 * client IP, and whichever of phone/token/installationId/deviceId/query/userId
 * are relevant to the policy being checked. Fields the current policy doesn't
 * need are simply left null.
 */
@Getter
@Builder
public class RateLimitRequestContext {
    private final String ip;
    private final String phoneNumber;
    private final String refreshToken;
    private final String installationId;
    private final String deviceId;
    private final String query;
    private final Long userId;
    /**
     * Raw canonicalized JSON body (sorted keys, deterministic), only populated
     * for body-fingerprint-aware policies (e.g. PUBLIC_CALCULATOR_WRITE). Never
     * logged directly - RateLimitKeyResolver SHA-256 hashes it before it ever
     * becomes bucket key material.
     */
    private final String bodyFingerprint;
}
