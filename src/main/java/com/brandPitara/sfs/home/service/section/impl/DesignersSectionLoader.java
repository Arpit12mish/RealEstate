package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.company.dto.ArchitectDesignerCardDto;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import com.brandPitara.sfs.company.enums.ArchitectDesignerType;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.company.repository.CompanyStatRepository;
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

  // Phase 8A-G: sourced from ArchitectDesignerType - see ArchitectsSectionLoader's
  // matching comment. Same accepted string set as before, no behavior change.
  private static final Set<String> ALLOWED_TYPES = ArchitectDesignerType.INTERIOR_DESIGNER.storageValues();

  private static final String STAT_PROJECTS_COMPLETED = "PROJECTS COMPLETED";
  private static final String STAT_YEARS_EXPERIENCE = "YEARS EXPERIENCE";
  private static final String STAT_CITIES_SERVED = "CITIES SERVED";
  private static final Set<String> AWARD_LABELS =
      Set.of("AWARDS", "AWARDS WON", "TOTAL AWARDS");

  private final CompanyRepository companyRepository;
  private final CompanyProjectRepository companyProjectRepository;
  private final CompanyStatRepository companyStatRepository;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.DESIGNERS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    List<CompanyEntity> companies = companyRepository
        .findByActiveTrueAndPublishedTrueAndDeletedFalse(
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

    Map<Long, Map<String, String>> statMapByCompanyId = buildStatMap(companyIds);

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
        .map(company -> toCard(
            company,
            statMapByCompanyId.getOrDefault(company.getId(), Map.of()),
            topProjectByCompanyId.get(company.getId())
        ))
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

  private Map<Long, Map<String, String>> buildStatMap(List<Long> companyIds) {
    Map<Long, Map<String, String>> statMapByCompanyId = new LinkedHashMap<>();

    List<CompanyStatEntity> stats = companyStatRepository
        .findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyIds);

    for (CompanyStatEntity stat : stats) {
      if (stat.getCompany() == null || stat.getCompany().getId() == null || stat.getLabel() == null) {
        continue;
      }

      Long companyId = stat.getCompany().getId();
      String normalizedLabel = normalize(stat.getLabel());

      statMapByCompanyId
          .computeIfAbsent(companyId, k -> new LinkedHashMap<>())
          .putIfAbsent(normalizedLabel, stat.getValue());
    }

    return statMapByCompanyId;
  }

  private ArchitectDesignerCardDto toCard(
      CompanyEntity company,
      Map<String, String> stats,
      CompanyProjectEntity topProject
  ) {
    return ArchitectDesignerCardDto.builder()
        .companyId(company.getId())
        .name(company.getName())
        .companyType(company.getCompanyType())
        .logoUrl(company.getLogoUrl())
        .coverImageUrl(resolveCoverImage(company, topProject))
        .specializationText(company.getSpecializationText())
        .projectsCompleted(stats.get(STAT_PROJECTS_COMPLETED))
        .yearsExperience(stats.get(STAT_YEARS_EXPERIENCE))
        .citiesServed(stats.get(STAT_CITIES_SERVED))
        .awardsCount(resolveAwardsCount(stats))
        .infoLine1(company.getInfoLine1())
        .infoLine2(company.getInfoLine2())
        .build();
  }

  private String resolveCoverImage(CompanyEntity company, CompanyProjectEntity topProject) {
    if (company.getCoverImageUrl() != null && !company.getCoverImageUrl().isBlank()) {
      return company.getCoverImageUrl();
    }
    return topProject != null ? topProject.getCoverMediaUrl() : null;
  }

  private String resolveAwardsCount(Map<String, String> stats) {
    for (String label : AWARD_LABELS) {
      if (stats.containsKey(label)) {
        return stats.get(label);
      }
    }
    return null;
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }
}