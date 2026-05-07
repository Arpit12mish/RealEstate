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
public class ProjectHighlightUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String title;

  @Size(max = 300)
  private String subtitle;

  @Size(max = 80)
  private String iconKey;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean active;
}