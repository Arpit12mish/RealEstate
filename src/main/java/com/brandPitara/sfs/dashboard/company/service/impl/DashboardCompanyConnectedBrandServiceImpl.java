package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyConnectedBrandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCompanyConnectedBrandServiceImpl implements DashboardCompanyConnectedBrandService {

  private final CompanyRepository companyRepository;
  private final BrandRepository brandRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional(readOnly = true)
  public List<CompanyConnectedBrandResponse> list(Long companyId) {
    assertCompanyExists(companyId);
    return fetchOrdered(companyId);
  }

  @Override
  @Transactional
  public CompanyConnectedBrandResponse create(Long companyId, CompanyConnectedBrandCreateRequest request) {
    CompanyEntity company = assertCompanyExists(companyId);

    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(request.getBrandId())
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + request.getBrandId()));

    boolean publicVisible = request.getPublicVisible() != null ? request.getPublicVisible() : true;
    if (publicVisible) {
      assertBrandIsPublishable(brand);
    }

    if (brandCollaborationRepository.existsByBrand_IdAndCompany_IdAndDeletedFalse(brand.getId(), companyId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This brand is already connected to this company"
      );
    }

    BrandCollaborationEntity entity = BrandCollaborationEntity.builder()
        .brand(brand)
        .targetType(BrandCollaborationTargetType.COMPANY)
        .company(company)
        .sourceType(BrandSourceType.ADMIN_ENTERED)
        .publicVisible(publicVisible)
        .verified(request.getVerified() != null ? request.getVerified() : false)
        .featured(request.getFeatured() != null ? request.getFeatured() : false)
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .title(clean(request.getTitle()))
        .description(clean(request.getDescription()))
        .active(true)
        .deleted(false)
        .build();

    BrandCollaborationEntity saved = brandCollaborationRepository.save(entity);
    bumpCaches();

    return toResponse(saved);
  }

  @Override
  @Transactional
  public CompanyConnectedBrandResponse update(
      Long companyId,
      Long collaborationId,
      CompanyConnectedBrandUpdateRequest request
  ) {
    BrandCollaborationEntity entity = findCollaborationOrThrow(companyId, collaborationId);

    if (request.getPublicVisible() != null) {
      if (request.getPublicVisible()) {
        assertBrandIsPublishable(entity.getBrand());
      }
      entity.setPublicVisible(request.getPublicVisible());
    }
    if (request.getVerified() != null) entity.setVerified(request.getVerified());
    if (request.getFeatured() != null) entity.setFeatured(request.getFeatured());
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getTitle() != null) entity.setTitle(clean(request.getTitle()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));

    BrandCollaborationEntity saved = brandCollaborationRepository.save(entity);
    bumpCaches();

    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long companyId, Long collaborationId) {
    BrandCollaborationEntity entity = findCollaborationOrThrow(companyId, collaborationId);

    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    brandCollaborationRepository.save(entity);

    bumpCaches();
  }

  // ---------- helpers ----------

  private CompanyEntity assertCompanyExists(Long companyId) {
    return companyRepository.findByIdAndDeletedFalse(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found: " + companyId));
  }

  private BrandCollaborationEntity findCollaborationOrThrow(Long companyId, Long collaborationId) {
    return brandCollaborationRepository
        .findByIdAndCompany_IdAndTargetTypeAndDeletedFalse(
            collaborationId, companyId, BrandCollaborationTargetType.COMPANY
        )
        .orElseThrow(() -> new EntityNotFoundException("Company connected brand not found: " + collaborationId));
  }

  private List<CompanyConnectedBrandResponse> fetchOrdered(Long companyId) {
    return brandCollaborationRepository
        .findByCompany_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(
            companyId, BrandCollaborationTargetType.COMPANY
        )
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private void assertBrandIsPublishable(BrandEntity brand) {
    boolean publishable = Boolean.TRUE.equals(brand.getPublished()) && Boolean.TRUE.equals(brand.getActive());
    if (!publishable) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Brand must be published and active to be publicly visible on a company profile"
      );
    }
  }

  private void bumpCaches() {
    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  private CompanyConnectedBrandResponse toResponse(BrandCollaborationEntity e) {
    BrandEntity brand = e.getBrand();
    return CompanyConnectedBrandResponse.builder()
        .id(e.getId())
        .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
        .brandId(brand.getId())
        .brandName(brand.getName())
        .brandSlug(brand.getSlug())
        .brandLogoUrl(brand.getLogoUrl())
        .brandPublished(Boolean.TRUE.equals(brand.getPublished()))
        .brandActive(Boolean.TRUE.equals(brand.getActive()))
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .featured(Boolean.TRUE.equals(e.getFeatured()))
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .title(e.getTitle())
        .description(e.getDescription())
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
