package com.brandPitara.sfs.provider.dto;

public record PresignProviderMediaResponse(
        String uploadUrl,
        String publicUrl,
        String key,
        int expiresInSeconds
) {}
