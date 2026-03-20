package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import lombok.*;

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
  private String distanceLabel;
  private String imageUrl;
  private int sortOrder;
  private boolean active;
}