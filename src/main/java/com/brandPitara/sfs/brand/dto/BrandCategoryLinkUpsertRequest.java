package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCategoryLinkUpsertRequest {

  @NotNull
  private Long categoryId;

  @Min(0) @Max(9999)
  private Integer displayOrder;

  private Boolean active;
}
