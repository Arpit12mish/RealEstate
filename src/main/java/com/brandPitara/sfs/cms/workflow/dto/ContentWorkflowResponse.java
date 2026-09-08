package com.brandPitara.sfs.cms.workflow.dto;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;

import java.time.OffsetDateTime;

public record ContentWorkflowResponse(
        Long contentId,
        ContentStatus status,
        Long version,
        Long currentReviewRevisionId,
        Long approvedRevisionId,
        Long currentPublishedRevisionId,
        OffsetDateTime publishedAt,
        Long publishedByDashboardUserId
) {
    public static ContentWorkflowResponse from(ContentPostEntity post) {
        return new ContentWorkflowResponse(
                post.getId(), post.getStatus(), post.getVersion(),
                post.getCurrentReviewRevision() == null ? null : post.getCurrentReviewRevision().getId(),
                post.getApprovedRevision() == null ? null : post.getApprovedRevision().getId(),
                post.getCurrentPublishedRevision() == null ? null : post.getCurrentPublishedRevision().getId(),
                post.getPublishedAt(),
                post.getPublishedBy() == null ? null : post.getPublishedBy().getId()
        );
    }
}
