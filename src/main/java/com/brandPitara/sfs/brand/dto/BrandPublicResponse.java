package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandPublicResponse {
  private Long id;
  private String name;
  private String logoUrl;
  private String description;
}
