package com.brandPitara.sfs.publicreview.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google.places")
public class GooglePlacesProperties {

    private String apiKey;
    private String baseUrl = "https://places.googleapis.com/v1";
    private String fieldMask = "id,displayName,formattedAddress,googleMapsUri,rating,userRatingCount,reviews";
    private String searchTextUrl = "https://places.googleapis.com/v1/places:searchText";
}
