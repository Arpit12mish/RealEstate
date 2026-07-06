package com.brandPitara.sfs.brand.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandDetailResponse {
  private Long id;
  private String name;
  private String slug;
  private String logoUrl;
  private String heroImageUrl;
  private String shortDescription;
  private String description;
  private List<PublicBrandCategoryResponse> categories;
  private PublicBrandStatsResponse stats;
  private List<PublicBrandCategoryResponse> productCategories;
  private List<PublicBrandSkuResponse> latestProducts;
  private List<PublicCollaborationTargetSummary> topProjects;
  private List<PublicCollaborationTargetSummary> topBuilders;
  private List<PublicCollaborationTargetSummary> topArchitects;
  private List<PublicCollaborationTargetSummary> interiorDesigners;
  private List<PublicBrandCertificateResponse> certificates;
  private List<PublicBrandFaqResponse> faqs;
  private List<PublicRelatedBrandResponse> relatedBrands;
}
