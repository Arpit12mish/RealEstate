package com.brandPitara.sfs.dashboard.review.dto;

import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkFieldReviewIssueRequest {

    @NotNull(message = "Entity type is required")
    private ReviewEntityType entityType;

    @NotNull(message = "Entity id is required")
    private Long entityId;

    @NotBlank(message = "Field key is required")
    @Size(max = 120, message = "Field key must be less than 120 characters")
    private String fieldKey;

    @Size(max = 180, message = "Field label must be less than 180 characters")
    private String fieldLabel;

    /**
     * Allowed from API:
     * RECHECK
     * WRONG
     *
     * FIXED is not allowed here. FIXED has a separate endpoint.
     */
    @NotNull(message = "Field review status is required")
    private FieldReviewStatus status;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;
}