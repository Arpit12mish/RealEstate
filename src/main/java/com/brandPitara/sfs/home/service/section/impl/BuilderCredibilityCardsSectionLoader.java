package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityCardResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
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
public class BuilderCredibilityCardsSectionLoader implements HomeSectionLoader {

  private final BuilderCredibilityService builderCredibilityService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.BUILDER_CREDIBILITY_CARDS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 12);

    List<BuilderCredibilityCardResponse> cards =
        builderCredibilityService.publicListCredibilityCards(ctx.cityId(), limit);

    if (cards == null || cards.isEmpty()) {
      return null;
    }

    return HomeSectionDto.<BuilderCredibilityCardResponse>builder()
        .type(HomeSectionType.BUILDER_CREDIBILITY_CARDS)
        .key("BUILDER_CREDIBILITY_CARDS")
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Trusted Builders")
        .items(cards)
        .build();
  }
}