package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisualAnalysisTagUpsertRequest {

  @NotBlank
  @Size(max = 60)
  private String label;

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$", message = "color must be a hex value like #3B7DDD")
  private String color;

  @Min(0) @Max(9999)
  private Integer sortOrder;
}
