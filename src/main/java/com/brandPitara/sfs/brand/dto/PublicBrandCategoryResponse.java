package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandCategoryResponse {
  private Long id;
  private String name;
  private String slug;
  private String iconUrl;
  private Integer displayOrder;
}
