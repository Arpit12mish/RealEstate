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
  // The product detail page on the brand's own external website; null when not set.
  private String externalUrl;
  // "View Product" when externalUrl is present, else null - lets mobile decide whether to
  // render the CTA button without re-deriving the label itself.
  private String ctaLabel;
}
