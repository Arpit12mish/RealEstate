package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCategoryLinkResponse {
  private Long id;
  private Long brandId;
  private Long categoryId;
  private String categoryName;
  private String categorySlug;
  private Integer displayOrder;
  private boolean active;
}
