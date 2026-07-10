package com.brandPitara.sfs.dashboard.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

// All fields optional/nullable - only non-null fields are applied (partial update).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyMediaUpdateRequest {

  private String mediaUrl;
  private String mediaType;

  // HERO | GALLERY | CARD
  private String usageType;

  @Size(max = 180)
  private String title;

  @Size(max = 255)
  private String altText;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean publicVisible;
  private Boolean active;
}
