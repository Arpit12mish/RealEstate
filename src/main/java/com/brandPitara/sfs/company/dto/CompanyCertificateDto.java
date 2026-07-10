package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCertificateDto {
  private Long id;
  private String title;
  private String issuer;
  private String certificateUrl;
  private Integer displayOrder;

  // Additive (Phase 4.8B) - existing mobile clients only read the fields above.
  private String description;
  private String fileUrl;
  private Integer year;
  private Boolean verified;
}