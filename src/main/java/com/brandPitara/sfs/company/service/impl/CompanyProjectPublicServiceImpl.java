package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.dto.PublicBrandConnectedResponse;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.service.CompanyProjectPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CompanyProjectPublicServiceImpl implements CompanyProjectPublicService {

  private static final int BRANDS_USED_LIMIT = 50;

  private final CompanyProjectRepository repo;
  private final BrandCollaborationRepository brandCollaborationRepository;

  @Override
  public Page<CompanyProjectCardDto> publicListByCompany(Long companyId, Pageable pageable) {
    return repo.findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(companyId, pageable)
        .map(this::toCard);
  }

  @Override
  public CompanyProjectResponse publicGet(Long companyProjectId) {
    CompanyProjectEntity p = repo
        .findByIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndCompany_PublishedTrueAndCompany_ActiveTrueAndCompany_DeletedFalse(companyProjectId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Company project not found"));

    return toResponse(p);
  }

  private CompanyProjectCardDto toCard(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectCardDto.builder()
        .id(p.getId())
        .name(p.getName())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .projectCityLatitude(p.getCity() != null ? p.getCity().getLatitude() : null)
        .projectCityLongitude(p.getCity() != null ? p.getCity().getLongitude() : null)
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .build();
  }

  private CompanyProjectResponse toResponse(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    var brandsUsed = brandCollaborationRepository
        .findPublicByCompanyProjectId(p.getId(), PageRequest.of(0, BRANDS_USED_LIMIT))
        .stream()
        .map(this::toConnectedBrand)
        .toList();

    return CompanyProjectResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .slug(p.getSlug())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .projectCityLatitude(p.getCity() != null ? p.getCity().getLatitude() : null)
        .projectCityLongitude(p.getCity() != null ? p.getCity().getLongitude() : null)
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .description(p.getDescription())
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .brandsUsed(brandsUsed)
        .build();
  }

  private PublicBrandConnectedResponse toConnectedBrand(BrandCollaborationEntity collaboration) {
    BrandEntity brand = collaboration.getBrand();
    return PublicBrandConnectedResponse.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .shortDescription(brand.getShortDescription())
        .relationType(collaboration.getRelationType())
        .sourceType(collaboration.getSourceType())
        .verified(Boolean.TRUE.equals(collaboration.getVerified()))
        .featured(Boolean.TRUE.equals(collaboration.getFeatured()))
        .displayOrder(collaboration.getSortOrder() != null ? collaboration.getSortOrder() : 0)
        .build();
  }
}
