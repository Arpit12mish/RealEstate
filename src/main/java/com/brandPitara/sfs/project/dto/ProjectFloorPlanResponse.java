package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanResponse {
  private Long id;
  private Long projectId;
  private String title;
  private String floorCode;
  private String imageUrl;
  private String carpetArea;
  private String exclusiveArea;
  private String superArea;
  private String unitLabel;
  private String description;
  private int sortOrder;
  private boolean active;
}