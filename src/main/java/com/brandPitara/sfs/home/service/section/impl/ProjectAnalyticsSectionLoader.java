package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.dto.ProjectAnalyticsCardDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.ProjectAnalyticsRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectAnalyticsSectionLoader implements HomeSectionLoader {

  private final ProjectAnalyticsRepository projectAnalyticsRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.PROJECT_ANALYTICS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long categoryId = ctx.categoryId();
    if (categoryId == null) return empty(cfg);

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    var rows = (ctx.builderId() != null)
        ? projectAnalyticsRepository.findByCategory_IdAndBuilder_IdAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(categoryId, ctx.builderId())
        : List.<com.brandPitara.sfs.home.entity.ProjectAnalyticsEntity>of();

    if (rows.isEmpty()) {
      rows = projectAnalyticsRepository.findByCategory_IdAndBuilderIsNullAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(categoryId);
    }

    if (rows.isEmpty()) return empty(cfg);
    if (rows.size() > limit) rows = rows.subList(0, limit);

    List<ProjectAnalyticsCardDto> cards = rows.stream()
        .map(r -> ProjectAnalyticsCardDto.builder()
            .id(r.getId())
            .title(r.getTitle())
            .imageUrl(r.getImageUrl())
            .caption(r.getCaption())
            .build())
        .toList();

    return HomeSectionDto.<ProjectAnalyticsCardDto>builder()
        .type(supports())
        .title(cfg.getTitle())
        .items(cards)
        .build();
  }

  private HomeSectionDto<ProjectAnalyticsCardDto> empty(HomeSectionConfigEntity cfg) {
    return HomeSectionDto.<ProjectAnalyticsCardDto>builder()
        .type(supports())
        .title(cfg.getTitle())
        .items(List.of())
        .build();
  }
}
