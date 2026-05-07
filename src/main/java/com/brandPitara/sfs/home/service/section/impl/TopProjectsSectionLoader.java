package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.project.dto.ProjectCardDto;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.mapper.ProjectCardMapper;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TopProjectsSectionLoader implements HomeSectionLoader {

  private final ProjectRepository projectRepository;
  private final ProjectMediaRepository projectMediaRepository;
  private final ProjectFavoriteService projectFavoriteService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.TOP_PROJECTS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    PageRequest pageable = PageRequest.of(
        0,
        limit,
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );

    List<ProjectEntity> projects;

    if (ctx != null && ctx.cityId() != null) {
      projects = projectRepository
          .findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalse(ctx.cityId(), pageable)
          .getContent();
    } else {
      projects = List.of();
    }

    if (projects.isEmpty()) {
      projects = projectRepository
          .findByPublishedTrueAndActiveTrueAndDeletedFalse(pageable)
          .getContent();
    }

    var projectIds = projects.stream()
        .map(ProjectEntity::getId)
        .toList();

    var media = projectIds.isEmpty()
        ? List.<com.brandPitara.sfs.project.entity.ProjectMediaEntity>of()
        : projectMediaRepository.findActiveByProjectIds(projectIds);

    Map<Long, List<com.brandPitara.sfs.project.entity.ProjectMediaEntity>> mediaByProject =
        media.stream()
            .collect(Collectors.groupingBy(m -> m.getProject().getId()));

    List<ProjectCardDto> cards = projects.stream()
        .map(p -> ProjectCardMapper.toCard(
            p,
            mediaByProject.getOrDefault(p.getId(), List.of())
        ))
        .toList();

    projectFavoriteService.enrichProjectCards(cards);

    return HomeSectionDto.<ProjectCardDto>builder()
        .type(HomeSectionType.TOP_PROJECTS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Top Projects")
        .items(cards)
        .build();
  }
}