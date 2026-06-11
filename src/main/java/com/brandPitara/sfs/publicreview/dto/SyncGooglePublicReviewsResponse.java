package com.brandPitara.sfs.publicreview.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncGooglePublicReviewsResponse {
    private Long reviewPlaceId;
    private String googlePlaceId;
    private String placeName;
    private BigDecimal rating;
    private Integer userRatingCount;
    private Integer fetchedReviewSampleCount;
    private OffsetDateTime syncedAt;
}
