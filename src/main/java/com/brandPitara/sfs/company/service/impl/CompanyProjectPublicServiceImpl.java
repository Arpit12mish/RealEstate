package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.dto.PublicBrandConnectedResponse;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectMediaEntity;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.mapper.CompanyProjectCardMapper;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyProjectMediaRepository;
import com.brandPitara.sfs.company.service.CompanyProjectPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CompanyProjectPublicServiceImpl implements CompanyProjectPublicService {

  private static final int BRANDS_USED_LIMIT = 50;

  private final CompanyProjectRepository repo;
  private final CompanyProjectMediaRepository companyProjectMediaRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<CompanyProjectCardDto> publicListByCompany(Long companyId, Pageable pageable) {
    return repo.findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(companyId, pageable)
        .map(this::toCard);
  }

  @Override
  @Transactional(readOnly = true)
  public CompanyProjectResponse publicGet(Long companyProjectId) {
    CompanyProjectEntity p = repo
        .findPublicByIdWithCompanyAndCity(companyProjectId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Company project not found"));

    return toResponse(p);
  }

  private CompanyProjectCardDto toCard(CompanyProjectEntity p) {
    return CompanyProjectCardMapper.toCard(p);
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
        .shortDescription(p.getShortDescription())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .locationLabel(p.getLocationLabel())
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
        .stats(p.getStats())
        .budget(p.getBudget())
        .priceBreakdown(p.getPriceBreakdown())
        .clientRequirements(p.getClientRequirements())
        .designMaterials(p.getDesignMaterials())
        .mediaGallery(companyProjectMediaRepository
            .findByCompanyProject_IdAndActiveTrueAndDeletedFalseAndPublicVisibleTrueOrderBySortOrderAscIdAsc(p.getId())
            .stream().map(this::toMediaResponse).toList())
        .build();
  }

  private CompanyProjectMediaResponse toMediaResponse(CompanyProjectMediaEntity media) {
    return CompanyProjectMediaResponse.builder()
        .id(media.getId())
        .companyProjectId(media.getCompanyProject().getId())
        .mediaUrl(media.getMediaUrl())
        .mediaType(media.getMediaType())
        .categoryKey(media.getCategoryKey())
        .categoryLabel(media.getCategoryLabel())
        .title(media.getTitle())
        .description(media.getDescription())
        .metricLabel(media.getMetricLabel())
        .metricValue(media.getMetricValue())
        .sortOrder(media.getSortOrder())
        .publicVisible(media.getPublicVisible())
        .active(media.getActive())
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
