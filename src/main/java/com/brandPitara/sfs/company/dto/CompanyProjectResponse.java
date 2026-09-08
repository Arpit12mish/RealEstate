package com.brandPitara.sfs.company.dto;

import com.brandPitara.sfs.brand.dto.PublicBrandConnectedResponse;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectResponse {
  private Long id;
  private String name;
  private String slug;
  private String shortDescription;

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

  private String description;

  private String coverMediaUrl;
  private String coverMediaType;
  private List<PublicBrandConnectedResponse> brandsUsed;
  private List<CompanyProjectStatDto> stats;
  private CompanyProjectBudgetDto budget;
  private List<CompanyProjectPriceBreakdownItemDto> priceBreakdown;
  private List<CompanyProjectClientRequirementDto> clientRequirements;
  private CompanyProjectDesignMaterialsDto designMaterials;
  private List<CompanyProjectMediaResponse> mediaGallery;
}
