package com.brandPitara.sfs.cms.media.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record CmsMediaUploadResponse(
        Long mediaAssetId,
        String status,
        String uploadUrl,
        int expiresInSeconds,
        OffsetDateTime assetExpiresAt,
        Map<String, String> requiredHeaders
) {
}
