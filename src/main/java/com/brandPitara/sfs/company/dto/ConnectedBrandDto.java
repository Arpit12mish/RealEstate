package com.brandPitara.sfs.company.dto;

import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedBrandDto {
  private Long brandId;
  private String name;
  private String slug;
  private String logoUrl;
  private String shortDescription;
  private String description;
  private BrandRelationType relationType;
  private BrandSourceType sourceType;
  private boolean verified;
  private boolean featured;
  private int displayOrder;
}
