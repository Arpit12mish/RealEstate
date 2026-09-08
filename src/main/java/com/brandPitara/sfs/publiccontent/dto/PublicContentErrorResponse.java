package com.brandPitara.sfs.publiccontent.dto;

import java.time.OffsetDateTime;

public record PublicContentErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
