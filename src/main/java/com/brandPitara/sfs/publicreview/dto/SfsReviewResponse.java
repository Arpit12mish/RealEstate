package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.SfsReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SfsReviewResponse {
    private Long id;
    private Long projectId;
    private String reviewerName;
    private Integer rating;
    private String headline;
    private String reviewText;
    private SfsReviewSourceType sourceType;
    private SfsReviewVerificationStatus verificationStatus;
    private String category;
    private String sentiment;
    private Boolean isFeatured;
    private String displayStatus;
    private String internalNote;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean submittedByUser;
    private String message;
}
