package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandCollaborationResponse;
import com.brandPitara.sfs.brand.dto.BrandCollaborationUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandCollaborationService;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.BusinessRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BrandCollaborationServiceImpl implements BrandCollaborationService {

  private final BrandRepository brandRepository;
  private final ProjectRepository projectRepository;
  private final BuilderRepository builderRepository;
  private final CompanyRepository companyRepository;
  private final CompanyProjectRepository companyProjectRepository;
  private final BusinessRepository businessRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BrandCollaborationResponse create(Long brandId, BrandCollaborationUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    BrandCollaborationEntity entity = BrandCollaborationEntity.builder()
        .brand(brand)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    applyTarget(entity, brandId, request.getTargetType(), request.getTargetId(), null);
    applyOptionalFields(entity, request);
    if (entity.getSourceType() == null) {
      entity.setSourceType(BrandSourceType.ADMIN_ENTERED);
    }

    BrandCollaborationEntity saved = brandCollaborationRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandCollaborationResponse update(Long brandId, Long collaborationId, BrandCollaborationUpsertRequest request) {
    BrandCollaborationEntity entity = brandCollaborationRepository.findByIdAndBrand_IdAndDeletedFalse(collaborationId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Collaboration not found: " + collaborationId));

    applyTarget(entity, brandId, request.getTargetType(), request.getTargetId(), collaborationId);
    applyOptionalFields(entity, request);
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandCollaborationEntity saved = brandCollaborationRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandCollaborationResponse> adminList(Long brandId, Pageable pageable) {
    brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    return brandCollaborationRepository.findAdminPageByBrandId(brandId, pageable)
        .map(this::toResponse);
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long collaborationId) {
    BrandCollaborationEntity entity = brandCollaborationRepository.findByIdAndBrand_IdAndDeletedFalse(collaborationId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Collaboration not found: " + collaborationId));

    entity.setDeleted(true);
    entity.setActive(false);
    entity.setPublicVisible(false);
    brandCollaborationRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  // ---------- helpers ----------

  /**
   * Clears all target FKs, resolves the requested target by targetType, and sets only
   * the matching FK - never accepts raw project/builder/company/businessId from the request.
   * Rejects (404) if the target doesn't exist, and (409) if this brand already has a
   * non-deleted collaboration with that exact target.
   */
  private void applyTarget(
      BrandCollaborationEntity entity,
      Long brandId,
      BrandCollaborationTargetType targetType,
      Long targetId,
      Long excludeCollaborationId
  ) {
    if (targetType == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType is required");
    }
    if (targetId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetId is required");
    }

    entity.setProject(null);
    entity.setBuilder(null);
    entity.setCompany(null);
    entity.setBusiness(null);
    entity.setCompanyProject(null);
    entity.setTargetType(targetType);

    switch (targetType) {
      case PROJECT -> {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(targetId)
            .orElseThrow(() -> new EntityNotFoundException("Project not found: " + targetId));
        if (isDuplicate(excludeCollaborationId,
            () -> brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(brandId, targetId),
            id -> brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalseAndIdNot(brandId, targetId, id))) {
          throw duplicateConflict(targetType, targetId);
        }
        entity.setProject(project);
      }
      case BUILDER -> {
        BuilderEntity builder = builderRepository.findByIdAndDeletedFalse(targetId)
            .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + targetId));
        if (isDuplicate(excludeCollaborationId,
            () -> brandCollaborationRepository.existsByBrand_IdAndBuilder_IdAndDeletedFalse(brandId, targetId),
            id -> brandCollaborationRepository.existsByBrand_IdAndBuilder_IdAndDeletedFalseAndIdNot(brandId, targetId, id))) {
          throw duplicateConflict(targetType, targetId);
        }
        entity.setBuilder(builder);
      }
      case COMPANY -> {
        CompanyEntity company = companyRepository.findByIdAndDeletedFalse(targetId)
            .orElseThrow(() -> new EntityNotFoundException("Company not found: " + targetId));
        if (isDuplicate(excludeCollaborationId,
            () -> brandCollaborationRepository.existsByBrand_IdAndCompany_IdAndDeletedFalse(brandId, targetId),
            id -> brandCollaborationRepository.existsByBrand_IdAndCompany_IdAndDeletedFalseAndIdNot(brandId, targetId, id))) {
          throw duplicateConflict(targetType, targetId);
        }
        entity.setCompany(company);
      }
      case BUSINESS -> {
        BusinessEntity business = businessRepository.findByIdAndActiveTrue(targetId)
            .orElseThrow(() -> new EntityNotFoundException("Business not found: " + targetId));
        if (isDuplicate(excludeCollaborationId,
            () -> brandCollaborationRepository.existsByBrand_IdAndBusiness_IdAndDeletedFalse(brandId, targetId),
            id -> brandCollaborationRepository.existsByBrand_IdAndBusiness_IdAndDeletedFalseAndIdNot(brandId, targetId, id))) {
          throw duplicateConflict(targetType, targetId);
        }
        entity.setBusiness(business);
      }
      case COMPANY_PROJECT -> {
        CompanyProjectEntity companyProject = companyProjectRepository.findByIdAndDeletedFalse(targetId)
            .orElseThrow(() -> new EntityNotFoundException("Company project not found: " + targetId));
        if (isDuplicate(excludeCollaborationId,
            () -> brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(brandId, targetId),
            id -> brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalseAndIdNot(brandId, targetId, id))) {
          throw duplicateConflict(targetType, targetId);
        }
        entity.setCompanyProject(companyProject);
      }
    }
  }

  private boolean isDuplicate(
      Long excludeCollaborationId,
      java.util.function.Supplier<Boolean> existsCheck,
      java.util.function.Function<Long, Boolean> existsExcludingSelfCheck
  ) {
    return excludeCollaborationId == null
        ? existsCheck.get()
        : existsExcludingSelfCheck.apply(excludeCollaborationId);
  }

  private ResponseStatusException duplicateConflict(BrandCollaborationTargetType targetType, Long targetId) {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "An active collaboration already exists for this brand and " + targetType + " " + targetId
    );
  }

  private void applyOptionalFields(BrandCollaborationEntity entity, BrandCollaborationUpsertRequest request) {
    if (request.getRelationType() != null) entity.setRelationType(request.getRelationType());
    if (request.getSourceType() != null) entity.setSourceType(request.getSourceType());
    if (request.getUsageCategory() != null) entity.setUsageCategory(clean(request.getUsageCategory()));
    if (request.getTitle() != null) entity.setTitle(clean(request.getTitle()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getVerified() != null) entity.setVerified(request.getVerified());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getFeatured() != null) entity.setFeatured(request.getFeatured());
    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());
  }

  private BrandCollaborationResponse toResponse(BrandCollaborationEntity e) {
    Long targetId = null;
    String targetName = null;
    String targetLogoUrl = null;

    switch (e.getTargetType()) {
      case PROJECT -> {
        targetId = e.getProject().getId();
        targetName = e.getProject().getName();
      }
      case BUILDER -> {
        targetId = e.getBuilder().getId();
        targetName = e.getBuilder().getName();
        targetLogoUrl = e.getBuilder().getLogoUrl();
      }
      case COMPANY -> {
        targetId = e.getCompany().getId();
        targetName = e.getCompany().getName();
        targetLogoUrl = e.getCompany().getLogoUrl();
      }
      case BUSINESS -> {
        targetId = e.getBusiness().getId();
        targetName = e.getBusiness().getName();
      }
      case COMPANY_PROJECT -> {
        targetId = e.getCompanyProject().getId();
        targetName = e.getCompanyProject().getName();
      }
    }

    return BrandCollaborationResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .targetType(e.getTargetType())
        .targetId(targetId)
        .targetName(targetName)
        .targetLogoUrl(targetLogoUrl)
        .relationType(e.getRelationType())
        .sourceType(e.getSourceType())
        .usageCategory(e.getUsageCategory())
        .title(e.getTitle())
        .description(e.getDescription())
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .featured(Boolean.TRUE.equals(e.getFeatured()))
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
