package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyMediaCreateRequest {

  @NotBlank
  private String mediaUrl;

  // Defaults to "IMAGE" if omitted - the only supported value today.
  private String mediaType;

  // HERO | GALLERY | CARD
  @NotBlank
  private String usageType;

  @Size(max = 180)
  private String title;

  @Size(max = 255)
  private String altText;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  // Defaults to true on create if omitted.
  private Boolean publicVisible;
  private Boolean active;
}
