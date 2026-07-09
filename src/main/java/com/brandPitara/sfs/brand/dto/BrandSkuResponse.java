package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandSkuResponse {
  private Long id;
  private Long brandId;
  private String name;
  private String slug;
  private String skuCode;
  private Long categoryId;
  private String categoryName;
  private Long productCategoryId;
  private String productCategoryName;
  private String shortDescription;
  private String description;
  private String imageUrl;
  private String priceLabel;
  private String externalUrl;
  private boolean featured;
  private boolean latest;
  private int displayOrder;
  private boolean published;
  private boolean active;
}
