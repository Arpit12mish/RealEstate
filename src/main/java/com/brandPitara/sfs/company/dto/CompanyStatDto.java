package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyStatDto {
  private String label;
  private String value;

  // Additive (Phase 4.8B) - existing mobile clients only read label/value.
  private Long id;
  private String iconKey;
  private Integer sortOrder;
}