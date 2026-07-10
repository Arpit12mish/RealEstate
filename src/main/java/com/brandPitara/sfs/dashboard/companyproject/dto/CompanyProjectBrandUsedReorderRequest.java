package com.brandPitara.sfs.dashboard.companyproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectBrandUsedReorderRequest {

  @NotEmpty
  @Valid
  private List<Item> items;

  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Item {

    @NotNull
    private Long collaborationId;

    @NotNull
    @Min(0) @Max(9999)
    private Integer sortOrder;
  }
}
