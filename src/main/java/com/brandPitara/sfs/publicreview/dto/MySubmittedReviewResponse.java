package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MySubmittedReviewResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String reviewerName;
    private Integer rating;
    private String headline;
    private String reviewText;
    private SfsReviewVerificationStatus verificationStatus;
    private String displayStatus;
    private Boolean submittedByUser;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewedAt;
}
