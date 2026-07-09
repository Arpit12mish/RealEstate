package com.brandPitara.sfs.instagram.service;

import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelResponse;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@RequiredArgsConstructor
public class InstagramReelMapper {

    private static final int CAPTION_PREVIEW_LIMIT = 140;

    private final AppInstagramProperties appInstagramProperties;

    public PublicInstagramReelItemResponse toPublicResponse(
        InstagramReelEntity entity,
        InstagramReelCategory category
    ) {
        InstagramReelCategory resolvedCategory = category != null
            ? category
            : (entity.getCategoryOverride() != null ? entity.getCategoryOverride() : InstagramReelCategory.LATEST);

        return PublicInstagramReelItemResponse.builder()
            .id(entity.getId())
            .title(resolveTitle(entity))
            .captionPreview(captionPreview(entity.getCaption()))
            .thumbnailUrl(resolvePublicThumbnailUrl(entity))
            .previewVideoUrl(entity.getPreviewVideoUrl())
            .instagramUrl(entity.getInstagramUrl())
            .category(resolvedCategory)
            .viewCount(nonNull(entity.getViewCount()))
            .likeCount(nonNull(entity.getLikeCount()))
            .commentCount(nonNull(entity.getCommentCount()))
            .publishedAt(entity.getPublishedAt())
            .build();
    }

    public DashboardInstagramReelResponse toDashboardResponse(InstagramReelEntity entity) {
        return DashboardInstagramReelResponse.builder()
            .id(entity.getId())
            .instagramMediaId(entity.getInstagramMediaId())
            .caption(entity.getCaption())
            .title(entity.getTitle())
            .instagramUrl(entity.getInstagramUrl())
            .thumbnailUrl(resolveDisplayThumbnailUrl(entity))
            .sourceThumbnailUrl(entity.getSourceThumbnailUrl())
            .cachedThumbnailUrl(resolveSafeCachedThumbnailUrl(entity))
            .cachedThumbnailStorageKey(entity.getCachedThumbnailStorageKey())
            .thumbnailCachedAt(entity.getThumbnailCachedAt())
            .metaFetchedAt(entity.getMetaFetchedAt())
            .lastSyncStatus(entity.getLastSyncStatus())
            .lastSyncError(entity.getLastSyncError())
            .previewVideoUrl(entity.getPreviewVideoUrl())
            .mediaType(entity.getMediaType())
            .mediaProductType(entity.getMediaProductType())
            .publishedAt(entity.getPublishedAt())
            .viewCount(nonNull(entity.getViewCount()))
            .likeCount(nonNull(entity.getLikeCount()))
            .commentCount(nonNull(entity.getCommentCount()))
            .shareCount(nonNull(entity.getShareCount()))
            .saveCount(nonNull(entity.getSaveCount()))
            .trendingScore(entity.getTrendingScore())
            .categoryOverride(entity.getCategoryOverride())
            .displayOrder(entity.getDisplayOrder())
            .active(entity.getActive())
            .deleted(entity.getDeleted())
            .syncedFromMeta(entity.getSyncedFromMeta())
            .lastSyncedAt(entity.getLastSyncedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public String deriveTitle(String caption) {
        if (!StringUtils.hasText(caption)) {
            return null;
        }
        String firstLine = caption.trim().split("\\R", 2)[0].trim();
        if (firstLine.length() <= 180) {
            return firstLine;
        }
        return firstLine.substring(0, 177).trim() + "...";
    }

    private String resolveTitle(InstagramReelEntity entity) {
        if (StringUtils.hasText(entity.getTitle())) {
            return entity.getTitle();
        }
        return deriveTitle(entity.getCaption());
    }

    private String resolvePublicThumbnailUrl(InstagramReelEntity entity) {
        return resolveDisplayThumbnailUrl(entity);
    }

    private String resolveDisplayThumbnailUrl(InstagramReelEntity entity) {
        String cachedThumbnailUrl = resolveSafeCachedThumbnailUrl(entity);
        if (StringUtils.hasText(cachedThumbnailUrl)) {
            return cachedThumbnailUrl;
        }
        return appInstagramProperties.getFallbackThumbnailUrl();
    }

    private String resolveSafeCachedThumbnailUrl(InstagramReelEntity entity) {
        String cachedThumbnailUrl = entity.getCachedThumbnailUrl();
        if (StringUtils.hasText(cachedThumbnailUrl) && !isUnsafeExternalInstagramUrl(cachedThumbnailUrl)) {
            return cachedThumbnailUrl;
        }
        return null;
    }

    boolean isUnsafeExternalInstagramUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String normalized = url.toLowerCase();
        if (normalized.contains("x-amz-signature")
            || normalized.contains("oe=")
            || normalized.contains("_nc_")) {
            return true;
        }

        String host = host(url);
        if (!StringUtils.hasText(host)) {
            return normalized.contains("cdninstagram")
                || normalized.contains("scontent")
                || normalized.contains("lookaside")
                || normalized.contains("instagram");
        }
        return host.contains("cdninstagram")
            || host.contains("scontent")
            || host.contains("lookaside")
            || host.contains("instagram");
    }

    private String host(String url) {
        try {
            URI uri = new URI(url.trim());
            return uri.getHost() != null ? uri.getHost().toLowerCase() : null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String captionPreview(String caption) {
        if (!StringUtils.hasText(caption)) {
            return null;
        }
        String cleaned = caption.trim().replaceAll("\\s+", " ");
        if (cleaned.length() <= CAPTION_PREVIEW_LIMIT) {
            return cleaned;
        }
        return cleaned.substring(0, CAPTION_PREVIEW_LIMIT - 3).trim() + "...";
    }

    private Long nonNull(Long value) {
        return value != null ? value : 0L;
    }
}
