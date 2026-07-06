package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.dto.BrandCardDto;
import com.brandPitara.sfs.brand.mapper.BrandCardMapper;
import com.brandPitara.sfs.brand.service.BrandPublicService;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopBrandsSectionLoader implements HomeSectionLoader {

  // Delegates to BrandPublicService (Phase 1C) rather than querying BrandRepository
  // directly, so the home carousel reuses the same tested public-visibility filtering
  // and batched category/stat lookups as GET /api/brands - no duplicate N+1-prone logic.
  private final BrandPublicService brandPublicService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.TOP_BRANDS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 20);

    var brandCards = brandPublicService.listPublicBrands(
        null,
        null,
        PageRequest.of(0, limit, Sort.by("priority").ascending().and(Sort.by("id").descending()))
    ).getContent();

    var cards = brandCards.stream().map(BrandCardMapper::toCard).toList();

    return HomeSectionDto.<BrandCardDto>builder()
        .type(HomeSectionType.TOP_BRANDS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Top Brands")
        .items(cards)
        .build();
  }
}
