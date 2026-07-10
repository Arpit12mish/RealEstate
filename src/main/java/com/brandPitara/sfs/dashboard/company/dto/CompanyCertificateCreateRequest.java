package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyCertificateCreateRequest {

  @NotBlank
  @Size(max = 180)
  private String title;

  @Size(max = 180)
  private String issuer;

  private String description;
  private String certificateUrl;
  private String certificateFileUrl;

  private Integer year;

  private Boolean verified;
  // Defaults to true on create if omitted.
  private Boolean publicVisible;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean active;
}
