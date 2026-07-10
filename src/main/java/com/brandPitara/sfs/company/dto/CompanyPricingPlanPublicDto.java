package com.brandPitara.sfs.company.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPricingPlanPublicDto {
  private Long id;
  private String pricingType;
  private String planName;
  private BigDecimal priceAmount;
  private String currency;
  private String billingUnit;
  private String description;
  private List<String> features;
  private Integer sortOrder;
}
