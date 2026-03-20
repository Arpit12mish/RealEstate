package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAnalyticsSectionLoader implements HomeSectionLoader {

  private final ProjectMeterService projectMeterService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.PROJECT_ANALYTICS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 5);

    Pageable pageable = PageRequest.of(
        0,
        limit,
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );

    Page<ProjectMeterCardResponse> page =
        projectMeterService.publicListMeterCards(ctx.cityId(), pageable);

    return HomeSectionDto.<ProjectMeterCardResponse>builder()
        .type(HomeSectionType.PROJECT_ANALYTICS)
        .title(cfg.getTitle())
        .items(page.getContent())
        .build();
  }
}