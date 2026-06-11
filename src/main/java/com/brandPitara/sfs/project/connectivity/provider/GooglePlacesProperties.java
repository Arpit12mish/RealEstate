package com.brandPitara.sfs.project.connectivity.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "google.maps.places")
public class GooglePlacesProperties {
  private boolean enabled = false;
  private String apiKey;
  private String textSearchUrl = "https://places.googleapis.com/v1/places:searchText";
  private String defaultLanguageCode = "en";
  private String defaultRegionCode = "IN";
  private int timeoutMs = 5000;
  private int maxResults = 20;
  private int maxRadiusMeters = 10000;
}
