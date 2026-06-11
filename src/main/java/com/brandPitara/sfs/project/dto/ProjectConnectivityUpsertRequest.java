package com.brandPitara.sfs.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

  @Size(max = 2000)
  private String summary;

  @Min(100)
  @Max(50000)
  private Integer defaultRadiusMeters;

  private Boolean searchEnabled;

  private Boolean active;
}
