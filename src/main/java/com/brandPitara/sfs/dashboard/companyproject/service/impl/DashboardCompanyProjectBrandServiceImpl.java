package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedReorderRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedUpdateRequest;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectBrandService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardCompanyProjectBrandServiceImpl implements DashboardCompanyProjectBrandService {

  private final CompanyProjectRepository companyProjectRepository;
  private final BrandRepository brandRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional(readOnly = true)
  public List<CompanyProjectBrandUsedResponse> list(Long companyProjectId) {
    assertCompanyProjectExists(companyProjectId);
    return fetchOrdered(companyProjectId);
  }

  @Override
  @Transactional
  public CompanyProjectBrandUsedResponse create(Long companyProjectId, CompanyProjectBrandUsedCreateRequest request) {
    CompanyProjectEntity project = assertCompanyProjectExists(companyProjectId);

    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(request.getBrandId())
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + request.getBrandId()));

    boolean publicVisible = request.getPublicVisible() != null ? request.getPublicVisible() : true;
    if (publicVisible) {
      assertBrandIsPublishable(brand);
    }

    if (brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(brand.getId(), companyProjectId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This brand is already attached to this company project"
      );
    }

    BrandCollaborationEntity entity = BrandCollaborationEntity.builder()
        .brand(brand)
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .companyProject(project)
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
  public CompanyProjectBrandUsedResponse update(
      Long companyProjectId,
      Long collaborationId,
      CompanyProjectBrandUsedUpdateRequest request
  ) {
    BrandCollaborationEntity entity = findCollaborationOrThrow(companyProjectId, collaborationId);

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
  public void delete(Long companyProjectId, Long collaborationId) {
    BrandCollaborationEntity entity = findCollaborationOrThrow(companyProjectId, collaborationId);

    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    brandCollaborationRepository.save(entity);

    bumpCaches();
  }

  @Override
  @Transactional
  public List<CompanyProjectBrandUsedResponse> reorder(Long companyProjectId, CompanyProjectBrandUsedReorderRequest request) {
    assertCompanyProjectExists(companyProjectId);

    Map<Long, Integer> sortOrderByCollaborationId = request.getItems().stream()
        .collect(Collectors.toMap(
            CompanyProjectBrandUsedReorderRequest.Item::getCollaborationId,
            CompanyProjectBrandUsedReorderRequest.Item::getSortOrder
        ));

    for (Map.Entry<Long, Integer> entry : sortOrderByCollaborationId.entrySet()) {
      BrandCollaborationEntity entity = findCollaborationOrThrow(companyProjectId, entry.getKey());
      entity.setSortOrder(entry.getValue());
      brandCollaborationRepository.save(entity);
    }

    bumpCaches();

    return fetchOrdered(companyProjectId);
  }

  // ---------- helpers ----------

  private CompanyProjectEntity assertCompanyProjectExists(Long companyProjectId) {
    return companyProjectRepository.findByIdAndDeletedFalse(companyProjectId)
        .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + companyProjectId));
  }

  private BrandCollaborationEntity findCollaborationOrThrow(Long companyProjectId, Long collaborationId) {
    return brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(
            collaborationId, companyProjectId, BrandCollaborationTargetType.COMPANY_PROJECT
        )
        .orElseThrow(() -> new EntityNotFoundException("Company project brand not found: " + collaborationId));
  }

  private List<CompanyProjectBrandUsedResponse> fetchOrdered(Long companyProjectId) {
    return brandCollaborationRepository
        .findByCompanyProject_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(
            companyProjectId, BrandCollaborationTargetType.COMPANY_PROJECT
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
          "Brand must be published and active to be publicly visible on a company project"
      );
    }
  }

  private void bumpCaches() {
    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  private CompanyProjectBrandUsedResponse toResponse(BrandCollaborationEntity e) {
    BrandEntity brand = e.getBrand();
    return CompanyProjectBrandUsedResponse.builder()
        .id(e.getId())
        .companyProjectId(e.getCompanyProject() != null ? e.getCompanyProject().getId() : null)
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
