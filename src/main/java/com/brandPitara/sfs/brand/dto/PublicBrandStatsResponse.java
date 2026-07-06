package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandStatsResponse {
  private long projectsCount;
  private long buildersCount;
  private long productsCount;
  private long designersCount;
  private long architectsCount;
}
