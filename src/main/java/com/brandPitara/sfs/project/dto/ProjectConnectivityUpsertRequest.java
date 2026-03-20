package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityUpsertRequest {
  private String title;
  private String subtitle;
  private String mapImageUrl;
  private Boolean active;
}