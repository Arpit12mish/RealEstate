package com.brandPitara.sfs.provider.dto;

public record ProviderMediaResponse(
        Long id,
        String mediaType,
        String url,
        String thumbnailUrl,
        int sortOrder
) {}
