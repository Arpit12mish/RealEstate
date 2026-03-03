package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.dto.ProjectPlanCardDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.ProjectPlanRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectPlanSectionLoader implements HomeSectionLoader {

  private final ProjectPlanRepository projectPlanRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.PROJECT_PLAN;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long categoryId = ctx.categoryId();
    if (categoryId == null) return empty(cfg);

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 5);

    var rows = (ctx.builderId() != null)
        ? projectPlanRepository.findByCategory_IdAndBuilder_IdAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(categoryId, ctx.builderId())
        : List.<com.brandPitara.sfs.home.entity.ProjectPlanEntity>of();

    if (rows.isEmpty()) {
      rows = projectPlanRepository.findByCategory_IdAndBuilderIsNullAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(categoryId);
    }

    if (rows.isEmpty()) return empty(cfg);
    if (rows.size() > limit) rows = rows.subList(0, limit);

    List<ProjectPlanCardDto> cards = rows.stream()
        .map(r -> ProjectPlanCardDto.builder()
            .imageUrl(r.getImageUrl())
            .title(r.getTitle())
            .description(r.getDescription())
            .companyLogoUrl(r.getCompanyLogoUrl())
            .tags(parseTags(r.getTags()))
            .build())
        .toList();

    return HomeSectionDto.<ProjectPlanCardDto>builder()
        .type(supports())
        .title(cfg.getTitle()) // section title
        .items(cards)
        .build();
  }

  private HomeSectionDto<ProjectPlanCardDto> empty(HomeSectionConfigEntity cfg) {
    return HomeSectionDto.<ProjectPlanCardDto>builder()
        .type(supports())
        .title(cfg.getTitle())
        .items(List.of())
        .build();
  }

  private List<String> parseTags(String csv) {
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}
