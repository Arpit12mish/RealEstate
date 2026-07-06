package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicRelatedBrandResponse {
  private Long id;
  private String name;
  private String slug;
  private String logoUrl;
  private String shortDescription;
}
