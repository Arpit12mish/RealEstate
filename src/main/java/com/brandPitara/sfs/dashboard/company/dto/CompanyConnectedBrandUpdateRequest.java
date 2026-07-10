package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

// All fields optional/nullable - only non-null fields are applied (partial update).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyConnectedBrandUpdateRequest {

  private Boolean publicVisible;
  private Boolean verified;
  private Boolean featured;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  @Size(max = 255)
  private String title;

  @Size(max = 4000)
  private String description;
}
