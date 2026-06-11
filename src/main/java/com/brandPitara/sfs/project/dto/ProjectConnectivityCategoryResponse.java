package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityCategoryResponse {
  private ProjectConnectivityCategory category;
  private String categoryLabel;
  private String iconKey;
  private Integer count;
  private List<ProjectConnectivityPlaceResponse> places;
}
