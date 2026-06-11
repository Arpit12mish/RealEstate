package com.brandPitara.sfs.publicreview.dto;

import com.brandPitara.sfs.publicreview.enums.PublicReviewPlaceCategory;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewDisplayMode;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewFetchStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewPlaceResponse {
    private Long id;
    private PublicReviewTargetType targetType;
    private Long targetId;
    private PublicReviewSourceType sourceType;
    private String googlePlaceId;
    private String placeName;
    private String formattedAddress;
    private String googleMapsUri;
    private PublicReviewPlaceCategory placeCategory;
    private Boolean active;
    private OffsetDateTime lastSyncedAt;
    private Boolean oneTimeFetched;
    private GoogleReviewFetchStatus fetchStatus;
    private GoogleReviewDisplayMode displayMode;
    private Boolean displayGoogleReviews;
    private BigDecimal rating;
    private Integer userRatingCount;
}
