package com.brandPitara.sfs.company.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyReviewSummaryDto {
  private BigDecimal rating;
  private Integer reviewCount;
  private String source;
  private String displayMode;
}
