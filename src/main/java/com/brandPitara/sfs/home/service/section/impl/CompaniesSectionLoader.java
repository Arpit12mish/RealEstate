package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.home.dto.CompanyItemDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionItemRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompaniesSectionLoader implements HomeSectionLoader {

  private final HomeSectionItemRepository homeSectionItemRepository;
  private final BrandRepository brandRepository;
  private final BuilderRepository builderRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.COMPANIES;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long categoryId = ctx.categoryId();
    if (categoryId == null) {
      return HomeSectionDto.<CompanyItemDto>builder()
          .type(supports())
          .title(cfg.getTitle() != null ? cfg.getTitle() : "Companies")
          .items(List.of())
          .build();
    }

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 12);

    // 1) Load curated item references from DB
    List<com.brandPitara.sfs.home.entity.HomeSectionItemEntity> items =
        homeSectionItemRepository
            .findByHomeCategory_IdAndSectionTypeAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
                categoryId, HomeSectionType.COMPANIES
            );

    if (items.isEmpty()) {
      return HomeSectionDto.<CompanyItemDto>builder()
          .type(supports())
          .title(cfg.getTitle() != null ? cfg.getTitle() : "Companies")
          .items(List.of())
          .build();
    }

    if (items.size() > limit) items = items.subList(0, limit);

    // 2) Split ids by type
    List<Long> brandIds = items.stream()
        .filter(i -> i.getItemType() == HomeSectionItemType.BRAND)
        .map(com.brandPitara.sfs.home.entity.HomeSectionItemEntity::getRefId)
        .distinct()
        .toList();

    List<Long> builderIds = items.stream()
        .filter(i -> i.getItemType() == HomeSectionItemType.BUILDER)
        .map(com.brandPitara.sfs.home.entity.HomeSectionItemEntity::getRefId)
        .distinct()
        .toList();

    // 3) Bulk fetch entities
List<BrandEntity> brands = brandIds.isEmpty()
    ? Collections.emptyList()
    : brandRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(brandIds);

List<BuilderEntity> builders = builderIds.isEmpty()
    ? Collections.emptyList()
    : builderRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(builderIds);

Map<Long, BrandEntity> brandMap = brands.stream()
    .collect(Collectors.toMap(BrandEntity::getId, Function.identity()));

Map<Long, BuilderEntity> builderMap = builders.stream()
    .collect(Collectors.toMap(BuilderEntity::getId, Function.identity()));

    // 4) Preserve DB order + build unified DTO list
    List<CompanyItemDto> cards = new ArrayList<>();

    for (var it : items) {
      if (it.getItemType() == HomeSectionItemType.BRAND) {
        var b = brandMap.get(it.getRefId());
        if (b == null) continue;

        cards.add(CompanyItemDto.builder()
            .entityType(HomeSectionItemType.BRAND)
            .id(b.getId())
            .name(b.getName())
            .logoUrl(b.getLogoUrl())
            .subtitle(it.getSubtitle())
            .build());
      }

      if (it.getItemType() == HomeSectionItemType.BUILDER) {
        var bu = builderMap.get(it.getRefId());
        if (bu == null) continue;

        cards.add(CompanyItemDto.builder()
            .entityType(HomeSectionItemType.BUILDER)
            .id(bu.getId())
            .name(bu.getName())
            .logoUrl(bu.getLogoUrl())
            .subtitle(it.getSubtitle())
            .build());
      }
    }

    return HomeSectionDto.<CompanyItemDto>builder()
        .type(supports())
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Companies")
        .items(cards)
        .build();
  }
}
