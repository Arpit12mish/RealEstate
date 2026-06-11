package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceResponse {
  private Long id;
  private Long projectId;

  private String placeName;
  private ProjectConnectivityType placeType;
  private String placeTypeLabel;

  private ProjectConnectivityCategory category;
  private String categoryLabel;

  private String distanceLabel;
  private Integer distanceMeters;

  private String durationLabel;
  private Integer durationSeconds;

  private Double latitude;
  private Double longitude;

  private String imageUrl;
  private String address;

  private String externalPlaceId;
  private String provider;

  private BigDecimal rating;
  private Integer userRatingCount;

  private int sortOrder;
  private boolean active;
  private boolean verified;
  private boolean featured;
}
