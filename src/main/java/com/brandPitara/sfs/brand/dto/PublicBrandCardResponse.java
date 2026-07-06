package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.PromoMediaType;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PublicBrandCardResponse {
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
  private boolean promoEnabled;
  private PromoMediaType promoMediaType;
  private String promoMediaUrl;
}
