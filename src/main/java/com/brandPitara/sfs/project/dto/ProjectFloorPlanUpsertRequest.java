package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String title;

  @Size(max = 50)
  private String floorCode;

  @NotBlank
  @Size(max = 500)
  private String imageUrl;

  @Size(max = 50)
  private String carpetArea;

  @Size(max = 50)
  private String exclusiveArea;

  @Size(max = 50)
  private String superArea;

  @Size(max = 80)
  private String unitLabel;

  @Size(max = 1000)
  private String description;

  @Min(0) @Max(9999)
  private Integer sortOrder;
  private Boolean active;
}