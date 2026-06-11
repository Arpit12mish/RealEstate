package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyResponse {
  private Long id;
  private String name;
  private String slug;

  private String companyType; // ARCHITECT, DESIGNER, etc

  private String logoUrl;
  private String coverImageUrl;

  private String description;

  private String phone;
  private String whatsapp;
}