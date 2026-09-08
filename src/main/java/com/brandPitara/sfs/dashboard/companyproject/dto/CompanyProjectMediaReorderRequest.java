package com.brandPitara.sfs.dashboard.companyproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectMediaReorderRequest {
  @NotEmpty @Valid private List<Item> items;

  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor @Builder
  public static class Item {
    @NotNull private Long mediaId;
    @NotNull @Min(0) @Max(9999) private Integer sortOrder;
  }
}
