package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.dto.PublicBrandCategoryResponse;
import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.ConnectedBrandDto;
import com.brandPitara.sfs.company.entity.CompanyBrandLinkEntity;
import com.brandPitara.sfs.company.repository.CompanyBrandLinkRepository;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyConnectedBrandPublicServiceImpl implements CompanyConnectedBrandPublicService {

  private final BrandCollaborationRepository brandCollaborationRepository;
  private final CompanyBrandLinkRepository companyBrandLinkRepository;
  private final BrandCategoryLinkRepository brandCategoryLinkRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ConnectedBrandDto> getConnectedBrands(Long companyId) {
    Set<Long> seenBrandIds = new LinkedHashSet<>();
    List<BrandCollaborationEntity> canonicalRows = new ArrayList<>();
    List<CompanyBrandLinkEntity> fallbackRows = new ArrayList<>();

    for (BrandCollaborationEntity collaboration : brandCollaborationRepository.findPublicByCompanyId(companyId)) {
      BrandEntity brand = collaboration.getBrand();
      if (brand == null || brand.getId() == null || !seenBrandIds.add(brand.getId())) {
        continue;
      }
      canonicalRows.add(collaboration);
    }

    for (CompanyBrandLinkEntity link : companyBrandLinkRepository
        .findPublicFallbackByCompanyId(companyId)) {
      BrandEntity brand = link.getBrand();
      if (brand == null || brand.getId() == null || !seenBrandIds.add(brand.getId())) {
        continue;
      }
      fallbackRows.add(link);
    }

    // Batch category lookup - one query for every connected brand instead of
    // one query per row, matching BrandPublicServiceImpl's batching pattern.
    Map<Long, List<PublicBrandCategoryResponse>> categoriesByBrandId = batchCategoriesByBrandIds(seenBrandIds);

    List<ConnectedBrandDto> result = new ArrayList<>();
    for (BrandCollaborationEntity collaboration : canonicalRows) {
      BrandEntity brand = collaboration.getBrand();
      result.add(fromCollaboration(collaboration, brand, categoriesByBrandId.getOrDefault(brand.getId(), List.of())));
    }
    for (CompanyBrandLinkEntity link : fallbackRows) {
      BrandEntity brand = link.getBrand();
      result.add(fromLegacyLink(link, brand, categoriesByBrandId.getOrDefault(brand.getId(), List.of())));
    }

    return result;
  }

  private Map<Long, List<PublicBrandCategoryResponse>> batchCategoriesByBrandIds(Collection<Long> brandIds) {
    if (brandIds.isEmpty()) return Map.of();

    return brandCategoryLinkRepository
        .findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandIds)
        .stream()
        .collect(Collectors.groupingBy(
            link -> link.getBrand().getId(),
            LinkedHashMap::new,
            Collectors.mapping(this::toCategoryResponse, Collectors.toList())
        ));
  }

  private PublicBrandCategoryResponse toCategoryResponse(BrandCategoryLinkEntity link) {
    var category = link.getCategory();
    return PublicBrandCategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .slug(category.getSlug())
        .iconUrl(null)
        .displayOrder(category.getPriority())
        .build();
  }

  private ConnectedBrandDto fromCollaboration(
      BrandCollaborationEntity collaboration,
      BrandEntity brand,
      List<PublicBrandCategoryResponse> categories) {
    return ConnectedBrandDto.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .heroImageUrl(brand.getHeroImageUrl())
        .shortDescription(brand.getShortDescription())
        .description(brand.getDescription())
        .categories(categories)
        .relationType(collaboration.getRelationType())
        .sourceType(collaboration.getSourceType())
        .verified(Boolean.TRUE.equals(collaboration.getVerified()))
        .featured(Boolean.TRUE.equals(collaboration.getFeatured()))
        .displayOrder(collaboration.getSortOrder() != null ? collaboration.getSortOrder() : 0)
        .build();
  }

  private ConnectedBrandDto fromLegacyLink(
      CompanyBrandLinkEntity link,
      BrandEntity brand,
      List<PublicBrandCategoryResponse> categories) {
    return ConnectedBrandDto.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .heroImageUrl(brand.getHeroImageUrl())
        .shortDescription(brand.getShortDescription())
        .description(brand.getDescription())
        .categories(categories)
        .verified(false)
        .featured(false)
        .displayOrder(link.getSortOrder() != null ? link.getSortOrder() : 0)
        .build();
  }
}
