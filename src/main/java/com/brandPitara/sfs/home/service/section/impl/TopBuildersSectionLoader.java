package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.builder.dto.BuilderCardDto;
import com.brandPitara.sfs.builder.mapper.BuilderCardMapper;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopBuildersSectionLoader implements HomeSectionLoader {

  private final BuilderRepository builderRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.TOP_BUILDERS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 20);

    Long cityId = ctx.cityId();

    var builders = (cityId == null)
        ? builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc()
        : builderRepository.findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseAndCity_IdOrderByPriorityAscIdDesc(cityId);

    if (builders.size() > limit) builders = builders.subList(0, limit);

    List<BuilderCardDto> cards = builders.stream()
        .map(BuilderCardMapper::toCard)
        .toList();

    return HomeSectionDto.<BuilderCardDto>builder()
        .type(HomeSectionType.TOP_BUILDERS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Top Builders")
        .items(cards)
        .build();
  }
}
