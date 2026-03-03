package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectCardDto {
  private Long id;
  private String name;

  private Long companyId;
  private String companyName;
  private String companyLogoUrl;

  private Long cityId;
  private String cityName;
  private String addressLine;

  private String coverMediaUrl;
  private String coverMediaType;
}