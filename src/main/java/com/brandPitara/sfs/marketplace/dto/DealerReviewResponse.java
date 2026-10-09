package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;

import java.time.OffsetDateTime;

public record DealerReviewResponse(
        Long id,
        Long dealerId,
        String reviewerName,
        String reviewerLocation,
        int rating,
        String reviewText,
        BusinessReviewStatus status,
        OffsetDateTime createdAt,
        String message
) {
}
