package com.brandPitara.sfs.brand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandProductCategoryUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  // Optional on create (auto-generated from name if blank) - always editable afterwards.
  @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase, alphanumeric, hyphen-separated")
  @Size(max = 180)
  private String slug;

  @Size(max = 4000)
  private String description;

  @Size(max = 500)
  private String imageUrl;

  // The brand's own product-category landing page. javascript:/data:/file:/tel:/mailto: and
  // any other non-http(s) scheme are rejected.
  @Pattern(regexp = "^https?://.+", message = "externalUrl must be a valid http/https URL")
  @Size(max = 500)
  private String externalUrl;

  private Boolean active;
  private Boolean publicVisible;

  @Min(0) @Max(9999)
  private Integer displayOrder;
}
