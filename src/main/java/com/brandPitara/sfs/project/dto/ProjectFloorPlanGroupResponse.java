package com.brandPitara.sfs.project.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanGroupResponse {
  private String groupKey;
  private String groupLabel;
  private List<ProjectFloorPlanResponse> items;
}