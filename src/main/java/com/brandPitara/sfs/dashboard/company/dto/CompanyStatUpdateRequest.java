package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

// All fields optional/nullable - only non-null fields are applied (partial update).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyStatUpdateRequest {

  @Size(max = 120)
  private String label;

  @Size(max = 180)
  private String value;

  @Size(max = 60)
  private String iconKey;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean publicVisible;
  private Boolean active;
}
