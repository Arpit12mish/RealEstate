package com.brandPitara.sfs.mobileupdate.exception;

import java.time.OffsetDateTime;

public record MobileUpdatePolicyErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
