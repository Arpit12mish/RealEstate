package com.brandPitara.sfs.company.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

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
  private Double projectCityLatitude;
  private Double projectCityLongitude;

  private String clientName;
  private String projectArea;
  private String detail3;
  private List<String> tags;

  private String coverMediaUrl;
  private String coverMediaType;
}