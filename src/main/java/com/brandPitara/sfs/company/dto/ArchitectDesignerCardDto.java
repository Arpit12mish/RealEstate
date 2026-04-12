package com.brandPitara.sfs.company.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectDesignerCardDto {
  private Long companyId;

  private String name;
  private String companyType;

  private String logoUrl;
  private String projectImageUrl;

  private String cityName;
  private String addressLine;
    private Double projectCityLatitude;
    private Double projectCityLongitude;

  private String projectsCompleted;

  private String detail1; // clientName
  private String detail2; // projectArea
  private String detail3; // optional

  private List<String> tags;
}