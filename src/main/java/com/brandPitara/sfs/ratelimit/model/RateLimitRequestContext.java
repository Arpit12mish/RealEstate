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
}
