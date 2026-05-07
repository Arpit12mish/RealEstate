package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityUpsertRequest {

  @Size(max = 150)
  private String title;

  @Size(max = 300)
  private String subtitle;

  @Size(max = 500)
  private String mapImageUrl;

  private Boolean active;
}