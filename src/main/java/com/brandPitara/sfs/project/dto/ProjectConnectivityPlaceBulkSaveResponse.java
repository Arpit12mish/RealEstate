package com.brandPitara.sfs.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceBulkSaveResponse {
  private Long projectId;
  private Integer requestedCount;
  private Integer savedCount;
  private Integer skippedDuplicateCount;
  private List<ProjectConnectivityPlaceResponse> savedPlaces;
  private List<ProjectConnectivityPlaceResponse> skippedDuplicates;
}
