package com.brandPitara.sfs.dashboard.media.dto;

import java.util.Map;

public record DashboardPresignUploadResponse(
        String uploadUrl,
        String publicUrl,
        String storageKey,
        int expiresInSeconds,
        Map<String, String> requiredHeaders
) {}
