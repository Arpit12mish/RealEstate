package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.PromoMediaType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCardDto {
  private Long id;
  private String name;
  private String logoUrl;

  private Boolean promoEnabled;
  private PromoMediaType promoMediaType;
  private String promoMediaUrl;
}
