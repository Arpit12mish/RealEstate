package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectResponse {
  private Long id;
  private String name;
  private String slug;

  private Long companyId;
  private String companyName;
  private String companyLogoUrl;

  private Long cityId;
  private String cityName;
  private String addressLine;

  private String description;

  private String coverMediaUrl;
  private String coverMediaType;
}