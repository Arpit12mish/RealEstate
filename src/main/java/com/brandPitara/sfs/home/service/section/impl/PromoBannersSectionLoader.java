package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromoBannersSectionLoader implements HomeSectionLoader {

  private final PromoBannerService promoBannerService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.PROMO_BANNERS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long fallbackCategoryId = (cfg.getHomeCategory() != null ? cfg.getHomeCategory().getId() : ctx.categoryId());

    // param1 can override bannerCategoryId. If null, use home category.
    Long bannerCategoryId = (cfg.getParam1() != null && !cfg.getParam1().isBlank())
        ? Long.valueOf(cfg.getParam1())
        : fallbackCategoryId;

    List<PromoBannerResponse> banners = promoBannerService.getBannersForCategory(bannerCategoryId);

    int limit = Math.max(0, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);
    if (banners.size() > limit) banners = banners.subList(0, limit);

    return HomeSectionDto.<PromoBannerResponse>builder()
        .type(HomeSectionType.PROMO_BANNERS)
        .title(cfg.getTitle()) // usually null (banner section has no title)
        .items(banners)
        .build();
  }
}
