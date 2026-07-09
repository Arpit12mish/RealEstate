package com.brandPitara.sfs.brand.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandStatsResponse {
  private Integer yearsInIndustry;
  private BigDecimal customerRating;
  private Integer customerRatingCount;
  private long projectsCount;
  private long buildersCount;
  private long productsCount;
  private long designersCount;
  private long architectsCount;
}
