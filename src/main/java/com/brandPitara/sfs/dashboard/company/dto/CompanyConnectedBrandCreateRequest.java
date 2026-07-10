package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyConnectedBrandCreateRequest {

  @NotNull
  private Long brandId;

  // Defaults to true on create if omitted.
  private Boolean publicVisible;

  // Defaults to false on create if omitted.
  private Boolean verified;

  // Defaults to false on create if omitted.
  private Boolean featured;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  @Size(max = 255)
  private String title;

  @Size(max = 4000)
  private String description;
}
