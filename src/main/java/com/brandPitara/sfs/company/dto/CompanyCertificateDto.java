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
}