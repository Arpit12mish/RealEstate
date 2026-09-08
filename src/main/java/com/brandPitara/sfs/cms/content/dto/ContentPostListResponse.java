package com.brandPitara.sfs.cms.content.dto;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostListView;

import java.time.OffsetDateTime;

public record ContentPostListResponse(
        Long id,
        ContentType contentType,
        ContentStatus status,
        String title,
        String slug,
        String excerpt,
        Long contentOwnerDashboardUserId,
        String contentOwnerDisplayName,
        Long updatedByDashboardUserId,
        String updatedByDisplayName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {
    public static ContentPostListResponse from(ContentPostEntity post) {
        return new ContentPostListResponse(
                post.getId(),
                post.getContentType(),
                post.getStatus(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getContentOwner().getId(),
                post.getContentOwner().getName(),
                post.getUpdatedBy().getId(),
                post.getUpdatedBy().getName(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getVersion()
        );
    }

    public static ContentPostListResponse from(ContentPostListView post) {
        return new ContentPostListResponse(
                post.getId(),
                post.getContentType(),
                post.getStatus(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getContentOwnerDashboardUserId(),
                post.getContentOwnerDisplayName(),
                post.getUpdatedByDashboardUserId(),
                post.getUpdatedByDisplayName(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getVersion()
        );
    }
}
