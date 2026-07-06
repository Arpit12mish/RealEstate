package com.brandPitara.sfs.brand.mapper;

import com.brandPitara.sfs.brand.dto.BrandCardDto;
import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.home.cards.dto.CardActionDto;

public class BrandCardMapper {

  public static BrandCardDto toCard(PublicBrandCardResponse b) {
    return BrandCardDto.builder()
        .id(b.getId())
        .name(b.getName())
        .slug(b.getSlug())
        .logoUrl(b.getLogoUrl())
        .heroImageUrl(b.getHeroImageUrl())
        .shortDescription(b.getShortDescription())
        .categories(b.getCategories())
        .productsCount(b.getProductsCount())
        .projectsCount(b.getProjectsCount())
        .buildersCount(b.getBuildersCount())
        .designersCount(b.getDesignersCount())
        .architectsCount(b.getArchitectsCount())
        .promoEnabled(b.isPromoEnabled())
        .promoMediaType(b.getPromoMediaType())
        .promoMediaUrl(b.getPromoMediaUrl())
        .action(CardActionDto.builder()
            .type("NAVIGATE")
            .target("BRAND_DETAIL")
            .path("/brands/" + b.getSlug())
            .build())
        .build();
  }
}
