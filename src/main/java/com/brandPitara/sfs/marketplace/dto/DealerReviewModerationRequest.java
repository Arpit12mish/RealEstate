package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DealerReviewModerationRequest(
        /** APPROVED or REJECTED; PENDING re-opens a decision. */
        @NotNull BusinessReviewStatus status,
        @Size(max = 2000) String note
) {
}
