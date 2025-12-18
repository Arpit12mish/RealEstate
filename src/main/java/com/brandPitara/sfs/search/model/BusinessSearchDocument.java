package com.brandPitara.sfs.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessSearchDocument {

    private Long id;

    private String name;

    private Long cityId;
    private String cityName;

    private Long categoryId;
    private String categoryName;

    private String addressText;

    private Double latitude;
    private Double longitude;

    private Double avgRating;
    private Integer totalRatings;

    private Boolean active;
    private Boolean sponsored;
    private Integer sponsoredPriority;

    // For "plumber & carpenter services", "near and fast", etc.
    private String keywords;

    // helper factory
    public static BusinessSearchDocument fromEntity(com.brandPitara.sfs.entity.BusinessEntity e) {
        StringBuilder addr = new StringBuilder();
        if (e.getAddressLine1() != null) addr.append(e.getAddressLine1()).append(" ");
        if (e.getAddressLine2() != null) addr.append(e.getAddressLine2()).append(" ");
        if (e.getLandmark() != null) addr.append(e.getLandmark()).append(" ");
        if (e.getPincode() != null) addr.append(e.getPincode());

        String keywords = String.join(" ",
                e.getName() != null ? e.getName() : "",
                e.getCategory() != null ? e.getCategory().getName() : "",
                e.getHighlightBadge() != null ? e.getHighlightBadge() : ""
        );

        return BusinessSearchDocument.builder()
                .id(e.getId())
                .name(e.getName())
                .cityId(e.getCity().getId())
                .cityName(e.getCity().getName())
                .categoryId(e.getCategory().getId())
                .categoryName(e.getCategory().getName())
                .addressText(addr.toString())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .avgRating(e.getAvgRating())
                .totalRatings(e.getTotalRatings())
                .active(e.getActive())
                .sponsored(e.getSponsored())
                .sponsoredPriority(e.getSponsoredPriority())
                .keywords(keywords)
                .build();
    }
}

