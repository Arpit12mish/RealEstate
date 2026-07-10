package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyPricingPlanReorderRequest {

  @NotEmpty
  @Valid
  private List<Item> items;

  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Item {

    @NotNull
    private Long planId;

    @NotNull
    @Min(0) @Max(9999)
    private Integer sortOrder;
  }
}
