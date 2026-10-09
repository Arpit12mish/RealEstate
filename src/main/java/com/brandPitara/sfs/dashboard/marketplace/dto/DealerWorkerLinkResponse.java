package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus;
import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import com.brandPitara.sfs.provider.enums.VerificationStatus;

import java.time.OffsetDateTime;

public record DealerWorkerLinkResponse(
        Long id,
        Long dealerId,
        Long workerId,
        String workerName,
        String workerTrade,
        VerificationStatus workerVerification,
        WorkerLinkStatus status,
        WorkerRecommendationStatus recommendationStatus,
        String recommendationNote,
        Long recommendationReviewedBy,
        OffsetDateTime recommendationReviewedAt,
        int sortOrder
) {
}
