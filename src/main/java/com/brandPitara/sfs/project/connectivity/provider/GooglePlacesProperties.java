package com.brandPitara.sfs.project.connectivity.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Getter
@Setter
@ConfigurationProperties(prefix = "google.maps.places")
@Validated
public class GooglePlacesProperties {
  private boolean enabled = false;
  private String apiKey;
  private String textSearchUrl = "https://places.googleapis.com/v1/places:searchText";
  private String defaultLanguageCode = "en";
  private String defaultRegionCode = "IN";
  @Min(1)
  private int connectTimeoutMs = 3000;
  @Min(1)
  private int readTimeoutMs = 5000;
  @Min(1)
  private int requestTimeoutMs = 8000;
  private int maxResults = 20;
  private int maxRadiusMeters = 10000;
}
