package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewSignalResponse {
    private PublicReviewTargetType targetType;
    private Long targetId;
    private PublicReviewSourceType sourceType;

    private BigDecimal rating;
    private Integer userRatingCount;

    private Integer positiveSampleCount;
    private Integer negativeSampleCount;
    private Integer neutralSampleCount;
    private Integer mixedSampleCount;

    private String sourceLabel;
    private String disclaimer;
    private OffsetDateTime lastSyncedAt;

    private List<PublicReviewPlaceResponse> places;
    private List<PublicReviewSampleResponse> samples;
}
