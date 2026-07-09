package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.brand.enums.PromoMediaType;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandResponse {
  private Long id;
  private String name;
  private String logoUrl;
  private String description;
  private String slug;
  private String heroImageUrl;
  private String shortDescription;
  private Integer foundedYear;
  private Integer yearsInIndustry;
  private BigDecimal customerRating;
  private Integer customerRatingCount;
  private List<Long> categoryIds;
  private boolean active;
  private boolean published;
  private int priority;
  private boolean promoEnabled;
  private PromoMediaType promoMediaType;
  private String promoMediaUrl;
}
