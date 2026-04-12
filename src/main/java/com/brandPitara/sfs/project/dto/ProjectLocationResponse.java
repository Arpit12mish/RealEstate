package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLocationResponse {
  private String addressLine;
  private Long cityId;
  private String cityName;
  private Double latitude;
  private Double longitude;
  private String mapImageUrl;
}