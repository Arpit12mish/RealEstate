package com.brandPitara.sfs.cms.media.dto;

import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;

import java.time.OffsetDateTime;

public record CmsMediaAssetResponse(
        Long id,
        String mediaType,
        String status,
        String filename,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        Long durationMillis,
        Long createdBy,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime readyAt,
        String failureCode,
        String previewUrl,
        Integer previewExpiresInSeconds
) {
    public static CmsMediaAssetResponse from(CmsMediaAssetEntity asset, String previewUrl, Integer previewExpiry) {
        return new CmsMediaAssetResponse(
                asset.getId(), asset.getMediaType().name(), asset.getStatus().name(),
                asset.getOriginalFilename(), asset.getContentType(), asset.getSizeBytes(),
                asset.getWidth(), asset.getHeight(), asset.getDurationMillis(),
                asset.getCreatedBy().getId(), asset.getCreatedBy().getName(),
                asset.getCreatedAt(), asset.getUpdatedAt(), asset.getReadyAt(), asset.getFailureCode(),
                previewUrl, previewExpiry
        );
    }
}
