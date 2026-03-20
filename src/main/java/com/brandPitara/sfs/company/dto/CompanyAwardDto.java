package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAwardDto {
  private Long id;
  private String title;
  private String subtitle;
  private String description;
  private Integer displayOrder;
}