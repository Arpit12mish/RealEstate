package com.brandPitara.sfs.cms.content.dto;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record ContentPostDetailResponse(
        Long id,
        ContentType contentType,
        ContentStatus status,
        String title,
        String slug,
        String excerpt,
        Integer readingTimeMinutes,
        Long publicAuthorId,
        Long categoryId,
        List<Long> tagIds,
        Long coverMediaAssetId,
        String coverAltText,
        Long contentOwnerDashboardUserId,
        String contentOwnerDisplayName,
        Long createdByDashboardUserId,
        String createdByDisplayName,
        Long updatedByDashboardUserId,
        String updatedByDisplayName,
        String seoTitle,
        String seoDescription,
        String canonicalUrl,
        boolean robotsIndex,
        boolean robotsFollow,
        Long currentReviewRevisionId,
        Long approvedRevisionId,
        Long currentPublishedRevisionId,
        OffsetDateTime publishedAt,
        Long publishedByDashboardUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {
    public ContentPostDetailResponse(Long id, ContentType contentType, ContentStatus status,
            String title, String slug, String excerpt,
            Long contentOwnerDashboardUserId, String contentOwnerDisplayName,
            Long createdByDashboardUserId, String createdByDisplayName,
            Long updatedByDashboardUserId, String updatedByDisplayName,
            String seoTitle, String seoDescription, String canonicalUrl,
            boolean robotsIndex, boolean robotsFollow,
            Long currentReviewRevisionId, Long approvedRevisionId, Long currentPublishedRevisionId,
            OffsetDateTime publishedAt, Long publishedByDashboardUserId,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, Long version) {
        this(id, contentType, status, title, slug, excerpt, null, null, null, List.of(), null, null,
                contentOwnerDashboardUserId, contentOwnerDisplayName, createdByDashboardUserId,
                createdByDisplayName, updatedByDashboardUserId, updatedByDisplayName,
                seoTitle, seoDescription, canonicalUrl, robotsIndex, robotsFollow,
                currentReviewRevisionId, approvedRevisionId, currentPublishedRevisionId,
                publishedAt, publishedByDashboardUserId, createdAt, updatedAt, version);
    }

    public static ContentPostDetailResponse from(ContentPostEntity post) {
        return new ContentPostDetailResponse(
                post.getId(),
                post.getContentType(),
                post.getStatus(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getReadingTimeMinutes(),
                post.getPublicAuthor() == null ? null : post.getPublicAuthor().getId(),
                post.getCategory() == null ? null : post.getCategory().getId(),
                post.getTags().stream().map(tag -> tag.getId()).sorted().toList(),
                post.getCoverMediaAsset() == null ? null : post.getCoverMediaAsset().getId(),
                post.getCoverAltText(),
                post.getContentOwner().getId(),
                post.getContentOwner().getName(),
                post.getCreatedBy().getId(),
                post.getCreatedBy().getName(),
                post.getUpdatedBy().getId(),
                post.getUpdatedBy().getName(),
                post.getSeoTitle(),
                post.getSeoDescription(),
                post.getCanonicalUrl(),
                Boolean.TRUE.equals(post.getRobotsIndex()),
                Boolean.TRUE.equals(post.getRobotsFollow()),
                post.getCurrentReviewRevision() == null ? null : post.getCurrentReviewRevision().getId(),
                post.getApprovedRevision() == null ? null : post.getApprovedRevision().getId(),
                post.getCurrentPublishedRevision() == null ? null : post.getCurrentPublishedRevision().getId(),
                post.getPublishedAt(),
                post.getPublishedBy() == null ? null : post.getPublishedBy().getId(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getVersion()
        );
    }
}
