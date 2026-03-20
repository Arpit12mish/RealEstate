package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanUpsertRequest {

  @NotBlank
  private String title;

  private String floorCode;

  @NotBlank
  private String imageUrl;

  private String carpetArea;
  private String exclusiveArea;
  private String superArea;
  private String unitLabel;
  private String description;

  private Integer sortOrder;
  private Boolean active;
}