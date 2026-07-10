package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyStatCreateRequest {

  @NotBlank
  @Size(max = 120)
  private String label;

  @NotBlank
  @Size(max = 180)
  private String value;

  @Size(max = 60)
  private String iconKey;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  // Defaults to true on create if omitted.
  private Boolean publicVisible;
  private Boolean active;
}
