package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyPricingPlanResponse {
  private Long id;
  private Long companyId;
  private String pricingType;
  private String planName;
  private BigDecimal priceAmount;
  private String currency;
  private String billingUnit;
  private String description;
  private List<String> features;
  private int sortOrder;
  private boolean publicVisible;
  private boolean active;
}
