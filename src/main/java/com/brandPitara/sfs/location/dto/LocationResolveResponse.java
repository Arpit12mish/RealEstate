package com.brandPitara.sfs.location.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResolveResponse {

    private boolean serviceable;

    private Long cityId;
    private String cityName;
    private String state;
    private String countryCode;

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;

    private Double nearestCityDistanceKm;

    private String message;
}