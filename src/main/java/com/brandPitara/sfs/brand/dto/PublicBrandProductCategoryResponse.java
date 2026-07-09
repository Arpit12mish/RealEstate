package com.brandPitara.sfs.brand.dto;

import lombok.*;

/**
 * A single brand's own product category (e.g. "Lamps", "Mirrors") on its detail page -
 * distinct from PublicBrandCategoryResponse, which represents the global brand-category
 * taxonomy (Paints/Electronics/Furniture) used for Connected Brands chips/filtering.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandProductCategoryResponse {
  private Long id;
  private String name;
  private String slug;
  private String description;
  private String imageUrl;
  private String externalUrl;
  private Integer sortOrder;
}
