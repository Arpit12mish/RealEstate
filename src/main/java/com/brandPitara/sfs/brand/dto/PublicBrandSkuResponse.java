package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandSkuResponse {
  private Long id;
  private String name;
  private String slug;
  private String skuCode;
  private Long categoryId;
  private String categoryName;
  private String shortDescription;
  private String imageUrl;
  private String priceLabel;
  private boolean featured;
  private boolean latest;
}
