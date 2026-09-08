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
  private String locationLabel;
  private Double projectCityLatitude;
  private Double projectCityLongitude;

  private String clientName;
  private String projectArea;
  private String detail3;
  private List<String> tags;

  // Additive, card-ready editorial fields for company profile portfolios.
  private String shortDescription;
  private String description;
  private String projectTypeLabel;
  private String areaLabel;
  private String budgetLabel;
  private List<CompanyProjectStatDto> stats;
  private CompanyProjectBudgetDto budget;

  private String coverMediaUrl;
  private String coverMediaType;
}
