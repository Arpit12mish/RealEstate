package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessReviewEntity;

public final class DealerReviewMapper {

    private DealerReviewMapper() {
    }

    public static DealerReviewResponse toResponse(BusinessReviewEntity r, String message) {
        return new DealerReviewResponse(
                r.getId(),
                r.getBusinessId(),
                r.getReviewerName(),
                r.getReviewerLocation(),
                r.getRating(),
                r.getReviewText(),
                r.getModerationStatus(),
                r.getCreatedAt(),
                message
        );
    }
}
