package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class BusinessResponse {

    private Long id;
    private String name;

    private String primaryPhone;
    private String secondaryPhone;
    private String whatsappPhone;

    // ready-to-use URLs for buttons
    private String callUrl;
    private String whatsappUrl;
    private String directionsUrl;

    // address / location
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String pincode;
    private Long   cityId;
    private String cityName;
    private String cityState;
    private Double latitude;
    private Double longitude;

    private Long   categoryId;
    private String categoryName;
    private String categorySlug;

    // timings
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean   openNow;          // true = OPEN, false = CLOSED, null = unknown
    private String    openStatusText;   // "OPEN", "CLOSED", "Closes at 10:00 pm" etc.

    // badges
    private Integer establishedYear;
    private Integer yearsInBusiness;    // computed
    private String  highlightBadge;     // e.g. "Best selling Panasonic TV"
    private Boolean topRated;           // show “Top Rated” chip
    private Boolean nearAndFast;        // show “Near & Fast” chip

    // ratings
    private Double  avgRating;
    private Integer totalRatings;

    private Long favoriteCount;  // total favorites across all users
    private Boolean isFavorite;  // current user has favorited this business
}
