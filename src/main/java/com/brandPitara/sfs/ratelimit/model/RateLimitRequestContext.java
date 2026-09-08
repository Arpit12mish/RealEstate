package com.brandPitara.sfs.ratelimit.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Raw (pre-key-building) identity material extracted from a single request:
 * validated primary identity, trusted client IP, and the small set of allowed
 * policy inputs. Legacy token/installation/device/query fields remain for API
 * compatibility with older unit fixtures but are deliberately ignored by the
 * production key resolver.
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
    private final String primaryIdentity;
    private final String abuseIp;
    /**
     * Raw canonicalized JSON body (sorted keys, deterministic), only populated
     * for body-fingerprint-aware policies (e.g. PUBLIC_CALCULATOR_WRITE). Never
     * logged directly - RateLimitKeyResolver SHA-256 hashes it before it ever
     * becomes bucket key material.
     */
    private final String bodyFingerprint;
}
