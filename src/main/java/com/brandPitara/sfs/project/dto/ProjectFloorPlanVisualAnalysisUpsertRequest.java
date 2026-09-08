package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanVisualAnalysisUpsertRequest {

  @Size(max = 160)
  private String title;

  @Size(max = 500)
  private String description;

  private FloorPlanVisualMediaType mediaType;

  private String mediaUrl;

  @Valid
  private List<VisualAnalysisTagUpsertRequest> tags;

  private Boolean active;
}
