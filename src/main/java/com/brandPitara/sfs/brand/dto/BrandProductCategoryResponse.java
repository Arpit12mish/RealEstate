package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandProductCategoryResponse {
  private Long id;
  private Long brandId;
  private String name;
  private String slug;
  private String description;
  private String imageUrl;
  private String externalUrl;
  private boolean active;
  private boolean publicVisible;
  private int displayOrder;
}
