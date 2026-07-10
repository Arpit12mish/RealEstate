package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

// All fields optional/nullable - only non-null fields are applied (partial update).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyCertificateUpdateRequest {

  @Size(max = 180)
  private String title;

  @Size(max = 180)
  private String issuer;

  private String description;
  private String certificateUrl;
  private String certificateFileUrl;

  private Integer year;

  private Boolean verified;
  private Boolean publicVisible;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean active;
}
