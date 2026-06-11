package com.brandPitara.sfs.publicreview.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GooglePlaceSearchResultItem {
    private String placeId;
    private String displayName;
    private String formattedAddress;
    private String googleMapsUri;
    private BigDecimal rating;
    private Integer userRatingCount;
}
