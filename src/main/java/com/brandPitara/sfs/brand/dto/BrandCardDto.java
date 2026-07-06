package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.PromoMediaType;
import com.brandPitara.sfs.home.cards.dto.CardActionDto;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandCardDto {
  private Long id;
  private String name;
  private String slug;
  private String logoUrl;
  private String heroImageUrl;
  private String shortDescription;
  private List<PublicBrandCategoryResponse> categories;
  private long productsCount;
  private long projectsCount;
  private long buildersCount;
  private long designersCount;
  private long architectsCount;

  private Boolean promoEnabled;
  private PromoMediaType promoMediaType;
  private String promoMediaUrl;

  // Deeplink to the brand detail screen, using the slug route (numeric is compatibility-only).
  private CardActionDto action;
}
