package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import jakarta.validation.constraints.NotNull;

/** Reviewer decision on a store recommendation: VERIFIED or REJECTED (PENDING re-opens it). */
public record RecommendationReviewRequest(@NotNull WorkerRecommendationStatus status) {
}
