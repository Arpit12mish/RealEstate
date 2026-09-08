package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanVisualAnalysisResponse {
  private String title;
  private String description;
  private FloorPlanVisualMediaType mediaType;
  private String mediaUrl;
  private List<VisualAnalysisTagResponse> tags;
}
