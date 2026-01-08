package com.brandPitara.sfs.provider.dto;

public record PresignProviderMediaResponse(
        String uploadUrl,
        String publicUrl,
        String storageKey,
        int expiresInSeconds
) {}
