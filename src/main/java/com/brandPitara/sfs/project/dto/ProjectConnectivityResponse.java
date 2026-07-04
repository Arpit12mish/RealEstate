package com.brandPitara.sfs.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityResponse {
  private Long projectId;
  private String title;
  private String subtitle;
  private String summary;
  private String mapImageUrl;

  private Double projectLatitude;
  private Double projectLongitude;
  private String projectAddress;

  private Integer defaultRadiusMeters;
  private Boolean searchEnabled;
  private Boolean active;

  private List<ProjectConnectivityPlaceResponse> places;
  private List<ProjectConnectivityCategoryResponse> categories;
}
