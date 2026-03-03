package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.dto.CategoryCardDto;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.mapper.CategoryCardMapper;
import com.brandPitara.sfs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopCategoriesSectionLoader implements HomeSectionLoader {

  private final CategoryRepository categoryRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.TOP_CATEGORIES;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 12);

    // param1: parentId override, else root categories
    List<CategoryEntity> cats;
    if (cfg.getParam1() != null && !cfg.getParam1().isBlank()) {
      Long parentId = Long.valueOf(cfg.getParam1());
      cats = categoryRepository.findByParentIdAndActiveTrueOrderByPriorityAsc(parentId);
    } else {
      cats = categoryRepository.findByParentIsNullAndActiveTrueOrderByPriorityAsc();
    }

    if (cats.size() > limit) cats = cats.subList(0, limit);

    List<CategoryCardDto> cards = cats.stream()
        .map(CategoryCardMapper::toCard)
        .toList();

    return HomeSectionDto.<CategoryCardDto>builder()
        .type(HomeSectionType.TOP_CATEGORIES)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Explore")
        .items(cards)
        .build();
  }
}
