package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.SfsReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SfsReviewCreateRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String reviewerName;

    private String headline;

    private String reviewText;

    private SfsReviewSourceType sourceType;

    private SfsReviewVerificationStatus verificationStatus;
}
