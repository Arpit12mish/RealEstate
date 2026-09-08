package com.brandPitara.sfs.publicreview.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google.places")
@Validated
public class GooglePlacesProperties {

    private String apiKey;
    private String baseUrl = "https://places.googleapis.com/v1";
    private String fieldMask = "id,displayName,formattedAddress,googleMapsUri,rating,userRatingCount,reviews";
    private String searchTextUrl = "https://places.googleapis.com/v1/places:searchText";

    @Min(1)
    private int connectTimeoutMs = 3000;

    /** Upper bound for waiting for response data. */
    @Min(1)
    private int readTimeoutMs = 5000;

    /** Upper bound for the complete HTTP exchange. */
    @Min(1)
    private int requestTimeoutMs = 8000;
}
