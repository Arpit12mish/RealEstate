package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectHighlightUpsertRequest {

  @NotBlank
  private String title;

  private String subtitle;

  private String iconKey;

  private Integer sortOrder;

  private Boolean active;
}