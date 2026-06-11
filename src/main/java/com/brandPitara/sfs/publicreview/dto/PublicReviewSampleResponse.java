package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.PublicReviewDisplayStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSentiment;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewSampleResponse {
    private Long id;
    private String reviewerName;
    private String reviewerProfileUrl;
    private String reviewerPhotoUrl;
    private Integer rating;
    private String reviewText;
    private String originalReviewText;
    private String languageCode;
    private String relativePublishTime;
    private OffsetDateTime publishTime;
    private PublicReviewSentiment sentiment;
    private String category;
    private PublicReviewDisplayStatus displayStatus;
}
