package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;

import java.time.OffsetDateTime;

/** Dashboard view of a review, including moderation internals that public APIs never expose. */
public record DealerReviewModerationResponse(
        Long id,
        Long dealerId,
        String dealerName,
        Long userId,
        String reviewerName,
        String reviewerLocation,
        int rating,
        String reviewText,
        BusinessReviewStatus status,
        String moderationNote,
        Long moderatedByDashboardUserId,
        OffsetDateTime moderatedAt,
        OffsetDateTime createdAt
) {
}
