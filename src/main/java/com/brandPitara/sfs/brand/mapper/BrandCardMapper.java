package com.brandPitara.sfs.brand.mapper;

import com.brandPitara.sfs.brand.dto.BrandCardDto;
import com.brandPitara.sfs.brand.entity.BrandEntity;

public class BrandCardMapper {
  public static BrandCardDto toCard(BrandEntity b) {
    return BrandCardDto.builder()
        .id(b.getId())
        .name(b.getName())
        .logoUrl(b.getLogoUrl())
        .promoEnabled(Boolean.TRUE.equals(b.getPromoEnabled()))
        .promoMediaType(b.getPromoMediaType())
        .promoMediaUrl(b.getPromoMediaUrl())
        .build();
  }
}
