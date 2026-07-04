package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.home.dto.GenericCardDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.entity.HomeSectionItemEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionItemRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenericCardSectionLoader implements HomeSectionLoader {

  private final HomeSectionItemRepository itemRepo;
  private final BrandRepository brandRepo;
  private final BuilderRepository builderRepo;
  private final CompanyRepository companyRepo;   // ✅ NEW
  private final ProjectRepository projectRepo;
  private final ProjectFavoriteService projectFavoriteService;

  @Override
  public HomeSectionType supports() {
    return HomeSectionType.GENERIC_CARDS;
  }

  @Override
  public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {

    Long homeCategoryId = (ctx.categoryId() == null) ? 0L : ctx.categoryId();
    String groupKey = cfg.getParam1();
    if (groupKey == null || groupKey.isBlank()) return empty(cfg);

    int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);

    // List<HomeSectionItemEntity> rows =
    //     itemRepo.findByHomeCategory_IdAndSectionTypeAndGroupKeyAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
    //         homeCategoryId, HomeSectionType.GENERIC_CARDS, groupKey
    //     );

    List<HomeSectionItemEntity> rows =
    itemRepo.findByConfig_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
        cfg.getId()
    );
    if (rows.isEmpty()) return empty(cfg);
    if (rows.size() > limit) rows = rows.subList(0, limit);

    // Collect ids
    List<Long> brandIds = rows.stream()
        .filter(r -> r.getItemType() == HomeSectionItemType.BRAND)
        .map(HomeSectionItemEntity::getRefId)
        .distinct()
        .toList();

    List<Long> builderIds = rows.stream()
        .filter(r -> r.getItemType() == HomeSectionItemType.BUILDER)
        .map(HomeSectionItemEntity::getRefId)
        .distinct()
        .toList();

    List<Long> businessIds = rows.stream()        // ✅ NEW
        .filter(r -> r.getItemType() == HomeSectionItemType.BUSINESS)
        .map(HomeSectionItemEntity::getRefId)
        .distinct()
        .toList();

    List<Long> projectIds = rows.stream()
    .filter(r -> r.getItemType() == HomeSectionItemType.PROJECT)
    .map(HomeSectionItemEntity::getRefId)
    .distinct()
    .toList();

    Map<Long, BrandEntity> brandsById = brandIds.isEmpty()
        ? Map.of()
        : brandRepo.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(brandIds)
            .stream().collect(Collectors.toMap(BrandEntity::getId, Function.identity()));

    Map<Long, BuilderEntity> buildersById = builderIds.isEmpty()
        ? Map.of()
        : builderRepo.findByIdInAndActiveTrueAndDeletedFalse(builderIds)
            .stream().collect(Collectors.toMap(BuilderEntity::getId, Function.identity()));

    Map<Long, CompanyEntity> companiesById = businessIds.isEmpty()   // ✅ NEW
        ? Map.of()
        : companyRepo.findByIdInAndActiveTrueAndPublishedTrueAndDeletedFalse(businessIds)
            .stream().collect(Collectors.toMap(CompanyEntity::getId, Function.identity()));

    Map<Long, ProjectEntity> projectsById = projectIds.isEmpty()
        ? Map.of()
        : projectRepo.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(projectIds)
            .stream()
            .collect(Collectors.toMap(ProjectEntity::getId, Function.identity()));

    List<GenericCardDto> cards = rows.stream()
        .map(r -> mapRowToCard(r, brandsById, buildersById, companiesById, projectsById))
        .filter(Objects::nonNull)
        .toList();

    projectFavoriteService.enrichGenericProjectCards(cards);

    return HomeSectionDto.<GenericCardDto>builder()
        .type(HomeSectionType.GENERIC_CARDS)
        .title(cfg.getTitle())
        .items(cards)
        .build();
  }

  private GenericCardDto mapRowToCard(
      HomeSectionItemEntity r,
      Map<Long, BrandEntity> brandsById,
      Map<Long, BuilderEntity> buildersById,
      Map<Long, CompanyEntity> companiesById,
      Map<Long, ProjectEntity> projectsById
  ) {

    // ================= BRAND =================
    if (r.getItemType() == HomeSectionItemType.BRAND) {
      BrandEntity b = brandsById.get(r.getRefId());
      if (b == null) {
        log.warn("GenericCard skipped: rowId={} BRAND refId={} not found", r.getId(), r.getRefId());
        return null;
      }

      String logoUrl = b.getLogoUrl();
      if (isBlank(logoUrl)) return null;

      return GenericCardDto.builder()
          .id(r.getId())               // row id
          .refId(b.getId())            // entity id
          .itemType(HomeSectionItemType.BRAND)
          .title(firstNonBlank(r.getTitle(), b.getName()))
          .subtitle(r.getSubtitle())
          .logoUrl(logoUrl)
          .imageUrl(r.getImageUrl())
          .build();
    }

    // ================= BUILDER =================
    if (r.getItemType() == HomeSectionItemType.BUILDER) {
      BuilderEntity bu = buildersById.get(r.getRefId());
      if (bu == null) {
        log.warn("GenericCard skipped: rowId={} BUILDER refId={} not found", r.getId(), r.getRefId());
        return null;
      }

      String logoUrl = bu.getLogoUrl();
      if (isBlank(logoUrl)) return null;

      return GenericCardDto.builder()
          .id(r.getId())
          .refId(bu.getId())
          .itemType(HomeSectionItemType.BUILDER)
          .title(firstNonBlank(r.getTitle(), bu.getName()))
          .subtitle(r.getSubtitle())
          .logoUrl(logoUrl)
          .imageUrl(r.getImageUrl())
          .build();
    }

    // ================= BUSINESS (Company) =================
    if (r.getItemType() == HomeSectionItemType.BUSINESS) {
      CompanyEntity c = companiesById.get(r.getRefId());
      if (c == null) {
        log.warn("GenericCard skipped: rowId={} BUSINESS refId={} not found", r.getId(), r.getRefId());
        return null;
      }

      String logoUrl = c.getLogoUrl();
      if (isBlank(logoUrl)) return null;

      return GenericCardDto.builder()
          .id(r.getId())
          .refId(c.getId())
          .itemType(HomeSectionItemType.BUSINESS)
          .title(firstNonBlank(r.getTitle(), c.getName()))
          .subtitle(r.getSubtitle())
          .logoUrl(logoUrl)
          .imageUrl(r.getImageUrl())
          .build();
    }

    if (r.getItemType() == HomeSectionItemType.PROJECT) {
      ProjectEntity p = projectsById.get(r.getRefId());
      if (p == null) {
        log.warn("GenericCard skipped: rowId={} PROJECT refId={} not found", r.getId(), r.getRefId());
        return null;
      }

      BuilderEntity b = p.getBuilder(); // loaded via @EntityGraph
      String logoUrl = (b != null) ? b.getLogoUrl() : null;
      if (isBlank(logoUrl)) return null; // UI needs logo

      // Project has no cover field, so we REQUIRE imageUrl at row-level for better UI
      String coverUrl = r.getImageUrl(); // may be null -> UI fallback letter

      return GenericCardDto.builder()
          .id(r.getId())
          .refId(p.getId())
          .itemType(HomeSectionItemType.PROJECT)
          .title(firstNonBlank(r.getTitle(), p.getName()))
          .subtitle(firstNonBlank(r.getSubtitle(), (b != null ? b.getName() : null)))
          .logoUrl(logoUrl)
          .imageUrl(coverUrl)
          .build();
    }

    return null;
  }



  private HomeSectionDto<GenericCardDto> empty(HomeSectionConfigEntity cfg) {
    return HomeSectionDto.<GenericCardDto>builder()
        .type(HomeSectionType.GENERIC_CARDS)
        .title(cfg.getTitle())
        .items(List.of())
        .build();
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String firstNonBlank(String a, String b) {
    return !isBlank(a) ? a : b;
  }
}
