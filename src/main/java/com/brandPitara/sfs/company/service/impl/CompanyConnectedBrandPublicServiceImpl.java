package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.ConnectedBrandDto;
import com.brandPitara.sfs.company.entity.CompanyBrandLinkEntity;
import com.brandPitara.sfs.company.repository.CompanyBrandLinkRepository;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyConnectedBrandPublicServiceImpl implements CompanyConnectedBrandPublicService {

  private final BrandCollaborationRepository brandCollaborationRepository;
  private final CompanyBrandLinkRepository companyBrandLinkRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ConnectedBrandDto> getConnectedBrands(Long companyId) {
    List<ConnectedBrandDto> result = new ArrayList<>();
    Set<Long> seenBrandIds = new LinkedHashSet<>();

    for (BrandCollaborationEntity collaboration : brandCollaborationRepository.findPublicByCompanyId(companyId)) {
      BrandEntity brand = collaboration.getBrand();
      if (brand == null || brand.getId() == null || !seenBrandIds.add(brand.getId())) {
        continue;
      }
      result.add(fromCollaboration(collaboration, brand));
    }

    for (CompanyBrandLinkEntity link : companyBrandLinkRepository
        .findPublicFallbackByCompanyId(companyId)) {
      BrandEntity brand = link.getBrand();
      if (brand == null || brand.getId() == null || !seenBrandIds.add(brand.getId())) {
        continue;
      }
      result.add(fromLegacyLink(link, brand));
    }

    return result;
  }

  private ConnectedBrandDto fromCollaboration(BrandCollaborationEntity collaboration, BrandEntity brand) {
    return ConnectedBrandDto.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .shortDescription(brand.getShortDescription())
        .description(brand.getDescription())
        .relationType(collaboration.getRelationType())
        .sourceType(collaboration.getSourceType())
        .verified(Boolean.TRUE.equals(collaboration.getVerified()))
        .featured(Boolean.TRUE.equals(collaboration.getFeatured()))
        .displayOrder(collaboration.getSortOrder() != null ? collaboration.getSortOrder() : 0)
        .build();
  }

  private ConnectedBrandDto fromLegacyLink(CompanyBrandLinkEntity link, BrandEntity brand) {
    return ConnectedBrandDto.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .shortDescription(brand.getShortDescription())
        .description(brand.getDescription())
        .verified(false)
        .featured(false)
        .displayOrder(link.getSortOrder() != null ? link.getSortOrder() : 0)
        .build();
  }
}
