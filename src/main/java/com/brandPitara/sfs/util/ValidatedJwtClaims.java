package com.brandPitara.sfs.util;

import java.time.Instant;

/** Immutable claims extracted from one signature/expiry-validated JWT parse. */
public record ValidatedJwtClaims(
        String subject,
        String principalType,
        Long userId,
        Long guestSessionId,
        String installationId,
        Instant expiresAt
) {
}
