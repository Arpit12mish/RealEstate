package com.brandPitara.sfs.dashboard.companyproject.dto;

import com.brandPitara.sfs.company.dto.*;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectDetailResponse {
  private Long id;
  private String name;
  private String slug;
  private String shortDescription;
  private Long companyId;
  private String companyName;
  private String companyType;
  private Long cityId;
  private String cityName;
  private String addressLine;
  private String locationLabel;
  private String clientName;
  private String projectArea;
  private String detail3;
  private List<String> tags;
  private String description;
  private String coverMediaUrl;
  private String coverMediaType;
  private List<CompanyProjectStatDto> stats;
  private CompanyProjectBudgetDto budget;
  private List<CompanyProjectPriceBreakdownItemDto> priceBreakdown;
  private List<CompanyProjectClientRequirementDto> clientRequirements;
  private CompanyProjectDesignMaterialsDto designMaterials;
  private boolean active;
  private boolean published;
  private int priority;
  private boolean deleted;
}
