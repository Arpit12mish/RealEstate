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

/**
 * Renamed from TopBrandsSectionLoader (Phase 1D.6). This is the home page's "Connected
 * Brands" showcase - brands connected/collaborated with projects, builders, architects,
 * and designers, not a generic top-brands list. TOP_BRANDS is kept in HomeSectionType as
 * deprecated/legacy, but this loader only supports CONNECTED_BRANDS; V123 renames the
 * existing config row so no live config still requests the old type.
 */
@Component
@RequiredArgsConstructor
public class ConnectedBrandsSectionLoader implements HomeSectionLoader {

  // Delegates to BrandPublicService (Phase 1C) so the home carousel reuses the same
  // tested public-visibility filtering and batched category/stat lookups as GET
  // /api/brands - no duplicate N+1-prone logic.
  private final BrandPublicService brandPublicService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.CONNECTED_BRANDS;
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
        .type(HomeSectionType.CONNECTED_BRANDS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Connected Brands")
        .items(cards)
        .build();
  }
}
