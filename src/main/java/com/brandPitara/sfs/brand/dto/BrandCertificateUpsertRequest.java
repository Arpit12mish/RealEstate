package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCertificateUpsertRequest {

  @NotBlank
  @Size(max = 180)
  private String title;

  @Size(max = 180)
  private String issuer;

  @Size(max = 500)
  private String certificateUrl;

  @Min(0) @Max(9999)
  private Integer displayOrder;

  private Boolean active;
}
