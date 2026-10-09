package com.brandPitara.sfs.marketplace.dto;

/** A VERIFIED store recommendation shown in the worker's "Recommended by" section. */
public record WorkerRecommendationResponse(
        Long dealerId,
        String dealerName,
        String dealerImageUrl,
        String locationText,
        String note
) {
}
