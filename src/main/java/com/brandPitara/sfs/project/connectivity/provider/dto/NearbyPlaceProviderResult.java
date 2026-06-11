package com.brandPitara.sfs.project.connectivity.provider.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyPlaceProviderResult {
  private String placeName;
  private ProjectConnectivityType placeType;
  private ProjectConnectivityCategory category;

  private Double latitude;
  private Double longitude;

  private Integer distanceMeters;
  private String distanceLabel;

  private Integer durationSeconds;
  private String durationLabel;

  private String address;

  private String provider;
  private String externalPlaceId;

  private BigDecimal rating;
  private Integer userRatingCount;

  private Boolean alreadySaved;
}
