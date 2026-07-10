package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

// All fields optional/nullable - only non-null fields are applied (partial update).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyPricingPlanUpdateRequest {

  // SUBSCRIPTION | PROJECT_BASED
  private String pricingType;

  @Size(max = 100)
  private String planName;

  @PositiveOrZero
  private BigDecimal priceAmount;

  @Size(max = 10)
  private String currency;

  @Size(max = 30)
  private String billingUnit;

  private String description;

  @Size(max = 20, message = "Max 20 features")
  private List<@Size(max = 255) String> features;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean publicVisible;
  private Boolean active;
}
