package com.brandPitara.sfs.cms.workflow.dto;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentMediaResponse;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import com.brandPitara.sfs.cms.workflow.domain.ContentTagSnapshot;

public record ContentRevisionDetailResponse(
        Long id,
        Long contentId,
        int revisionNumber,
        ContentType contentType,
        String title,
        String slug,
        String excerpt,
        Integer readingTimeMinutes,
        Long publicAuthorId,
        String publicAuthorName,
        String publicAuthorSlug,
        String publicAuthorDesignation,
        Long publicAuthorProfileMediaAssetId,
        Long categoryId,
        String categoryName,
        String categorySlug,
        List<ContentTagSnapshot> tags,
        Long coverMediaAssetId,
        String coverAltText,
        String seoTitle,
        String seoDescription,
        String canonicalUrl,
        boolean robotsIndex,
        boolean robotsFollow,
        ContentDocument document,
        short documentSchemaVersion,
        long createdFromPostVersion,
        ContentRevisionReason revisionReason,
        Long createdByDashboardUserId,
        String createdByDisplayName,
        OffsetDateTime createdAt,
        Map<Long, ContentDocumentMediaResponse> media
) {
    public static ContentRevisionDetailResponse from(
            ContentPostRevisionEntity revision,
            Map<Long, ContentDocumentMediaResponse> media
    ) {
        return new ContentRevisionDetailResponse(
                revision.getId(), revision.getContentPost().getId(), revision.getRevisionNumber(),
                revision.getContentType(), revision.getTitle(), revision.getSlug(), revision.getExcerpt(),
                revision.getReadingTimeMinutes(),
                revision.getPublicAuthorId(), revision.getPublicAuthorName(), revision.getPublicAuthorSlug(),
                revision.getPublicAuthorDesignation(), revision.getPublicAuthorProfileMediaAssetId(),
                revision.getCategoryId(), revision.getCategoryName(), revision.getCategorySlug(),
                revision.getTagSnapshots(), revision.getCoverMediaAssetId(), revision.getCoverAltText(),
                revision.getSeoTitle(), revision.getSeoDescription(), revision.getCanonicalUrl(),
                Boolean.TRUE.equals(revision.getRobotsIndex()), Boolean.TRUE.equals(revision.getRobotsFollow()),
                revision.getContentDocument(), revision.getContentDocumentSchemaVersion(),
                revision.getCreatedFromPostVersion(), revision.getRevisionReason(),
                revision.getCreatedBy().getId(), revision.getCreatedBy().getName(), revision.getCreatedAt(), media
        );
    }
}
