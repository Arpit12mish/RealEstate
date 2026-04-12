package com.brandPitara.sfs.company.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectDesignerDetailResponse {
  private Long companyId;
  private String name;
  private String slug;
  private String companyType;

  private String logoUrl;
  private String thumbnailImageUrl;
  private String description;

  private List<CompanyProjectCardDto> topProjects;
  private List<CompanyStatDto> stats;
  private List<CompanyAwardDto> awardsAndPublications;
  private List<CompanyCertificateDto> certificates;
  private java.util.List<ConnectedBrandDto> connectedBrands;
}