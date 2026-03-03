package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.FeaturedCarouselCardDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.FeaturedCarouselService;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FeaturedCarouselSectionLoader implements HomeSectionLoader {

  private final FeaturedCarouselService featuredCarouselService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.FEATURED_CAROUSEL;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    // ✅ record accessor
    if (ctx.categoryId() == null) return null;

    List<FeaturedCarouselCardDto> items =
        featuredCarouselService.getCarousel(ctx.cityId(), ctx.categoryId());

    if (items == null || items.isEmpty()) return null;

    return HomeSectionDto.<FeaturedCarouselCardDto>builder()
        .type(HomeSectionType.FEATURED_CAROUSEL)
        .title(resolveTitle(cfg))
        .items(items)
        .build();
  }

  private String resolveTitle(HomeSectionConfigEntity cfg) {
    if (cfg != null && cfg.getTitle() != null && !cfg.getTitle().isBlank()) {
      return cfg.getTitle();
    }
    return "Top Builders this week";
  }
}
