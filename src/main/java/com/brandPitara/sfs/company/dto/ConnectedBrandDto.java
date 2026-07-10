package com.brandPitara.sfs.company.dto;

import com.brandPitara.sfs.brand.dto.PublicBrandCategoryResponse;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import lombok.*;

import java.util.List;

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

  // Additive - brings this card up to parity with the Home screen's brand
  // card (BrandCardMapper/PublicBrandCardResponse) so detail-page connected
  // brand cards can render the same hero image + category chips.
  private String heroImageUrl;
  private List<PublicBrandCategoryResponse> categories;
}
