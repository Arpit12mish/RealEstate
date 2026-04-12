package com.brandPitara.sfs.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPricingSummaryResponse {
  private Long minPrice;
  private Long maxPrice;

  private Long averageAreaPrice;

  private Long estimatedMonthlyEmiMin;
  private Long estimatedMonthlyEmiMax;

  private Double appreciationPercent;

  private Integer downPaymentPercent;
  private Double annualInterestRate;
  private Integer tenureYears;
}