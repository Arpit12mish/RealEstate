package com.brandPitara.sfs.brand.dto;


import com.brandPitara.sfs.brand.enums.PromoMediaType;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandPromoResponse {
  private Long id;
  private boolean promoEnabled;
  private PromoMediaType promoMediaType;
  private String promoMediaUrl;
}
