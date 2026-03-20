package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.company.dto.ArchitectDesignerCardDto;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DesignersSectionLoader implements HomeSectionLoader {

  private static final Set<String> ALLOWED_TYPES =
      Set.of("DESIGNER", "DESIGNERS", "INTERIOR_DESIGNER");

  private final CompanyRepository companyRepository;
  private final CompanyProjectRepository companyProjectRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.DESIGNERS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    List<CompanyEntity> companies = companyRepository.findByActiveTrueAndPublishedTrueAndDeletedFalse(
            PageRequest.of(0, limit * 3, Sort.by("priority").ascending().and(Sort.by("id").descending()))
        )
        .getContent()
        .stream()
        .filter(this::isDesigner)
        .limit(limit)
        .toList();

    if (companies.isEmpty()) {
      return HomeSectionDto.<ArchitectDesignerCardDto>builder()
          .type(HomeSectionType.DESIGNERS)
          .title(cfg.getTitle() != null ? cfg.getTitle() : "Interior Designers")
          .items(List.of())
          .build();
    }

    List<Long> companyIds = companies.stream().map(CompanyEntity::getId).toList();

    Map<Long, CompanyProjectEntity> topProjectByCompanyId =
        companyProjectRepository
            .findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(companyIds)
            .stream()
            .collect(Collectors.toMap(
                p -> p.getCompany().getId(),
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

    List<ArchitectDesignerCardDto> cards = companies.stream()
        .map(company -> toCard(company, topProjectByCompanyId.get(company.getId())))
        .toList();

    return HomeSectionDto.<ArchitectDesignerCardDto>builder()
        .type(HomeSectionType.DESIGNERS)
        .title(cfg.getTitle() != null ? cfg.getTitle() : "Interior Designers")
        .items(cards)
        .build();
  }

  private boolean isDesigner(CompanyEntity company) {
    return company.getCompanyType() != null
        && ALLOWED_TYPES.contains(company.getCompanyType().trim().toUpperCase());
  }

  private ArchitectDesignerCardDto toCard(CompanyEntity company, CompanyProjectEntity project) {
    return ArchitectDesignerCardDto.builder()
        .companyId(company.getId())
        .name(company.getName())
        .companyType(company.getCompanyType())
        .logoUrl(company.getLogoUrl())
        .projectImageUrl(project != null ? project.getCoverMediaUrl() : company.getCoverImageUrl())
        .cityName(project != null && project.getCity() != null ? project.getCity().getName() : null)
        .addressLine(project != null ? project.getAddressLine() : null)
        .projectCityLatitude(project != null && project.getCity() != null ? project.getCity().getLatitude() : null)
        .projectCityLongitude(project != null && project.getCity() != null ? project.getCity().getLongitude() : null)
        .detail1(project != null ? project.getClientName() : null)
        .detail2(project != null ? project.getProjectArea() : null)
        .detail3(project != null ? project.getDetail3() : null)
        .tags(project != null ? CompanyProjectTagMapper.toTags(project.getTags()) : List.of())
        .build();
  }
}