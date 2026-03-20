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
  private String mapImageUrl;
  private List<ProjectConnectivityPlaceResponse> places;
}