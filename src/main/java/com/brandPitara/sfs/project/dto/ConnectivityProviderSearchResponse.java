package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.connectivity.provider.dto.NearbyPlaceProviderResult;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityProviderSearchResponse {
  private Long projectId;
  private ProjectConnectivityCategory category;
  private String categoryLabel;
  private String query;
  private Integer radiusMeters;
  private Double projectLatitude;
  private Double projectLongitude;
  private String provider;
  private List<NearbyPlaceProviderResult> results;
}
