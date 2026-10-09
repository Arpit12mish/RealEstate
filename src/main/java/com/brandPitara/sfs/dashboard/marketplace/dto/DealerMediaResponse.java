package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;

public record DealerMediaResponse(
        Long id,
        BusinessMediaUsage usageType,
        String mediaUrl,
        String storageKey,
        String altText,
        int sortOrder,
        boolean active
) {
}
