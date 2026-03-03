package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.dto.BrandCardDto;
import com.brandPitara.sfs.brand.mapper.BrandCardMapper;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ArchitectsAndDesignersSectionLoader implements HomeSectionLoader {

  private final BrandRepository brandRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.ARCHITECTS_AND_DESIGNERS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    // param1 = categoryId for "Architecture & Designers"
    if (cfg.getParam1() == null || cfg.getParam1().isBlank()) return empty(cfg);

    Long brandCategoryId = Long.valueOf(cfg.getParam1());

    var page = brandRepository.findByCategory_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
        brandCategoryId,
        PageRequest.of(0, limit, Sort.by("priority").ascending().and(Sort.by("id").descending()))
    );

    var cards = page.getContent().stream()
        .map(BrandCardMapper::toCard)
        .toList();

    if (cards.isEmpty()) return empty(cfg);

    return HomeSectionDto.<BrandCardDto>builder()
        .type(supports())
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Architecture and Designers")
        .items(cards)
        .build();
  }

  private HomeSectionDto<BrandCardDto> empty(HomeSectionConfigEntity cfg) {
    return HomeSectionDto.<BrandCardDto>builder()
        .type(supports())
        .title(cfg.getTitle())
        .items(List.of())
        .build();
  }
}
