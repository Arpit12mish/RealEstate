package com.brandPitara.sfs.request.dto;

import java.time.OffsetDateTime;

public record ServiceRequestInterestResponse(
        Long id,
        String status,
        OffsetDateTime createdAt,
        Provider provider,
        String message
) {
    public record Provider(
            Long providerId,
            String displayName,
            String primaryCategoryName,
            String providerType,
            String maskedPhone,
            String profilePhotoUrl
    ) {}
}
