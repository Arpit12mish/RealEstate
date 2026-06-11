package com.brandPitara.sfs.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivitySearchResponse {
  private Long projectId;
  private String query;
  private Double projectLatitude;
  private Double projectLongitude;
  private List<ProjectConnectivityPlaceResponse> results;
}
