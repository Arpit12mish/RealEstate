package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.PublicBrandConnectedResponse;
import com.brandPitara.sfs.brand.dto.PublicConnectedBrandsSectionResponse;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.service.BrandConnectedPublicService;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandConnectedPublicServiceImpl implements BrandConnectedPublicService {

  private static final int MAX_LIMIT = 50;

  private final ProjectRepository projectRepository;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
  private final BuilderRepository builderRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;

  @Override
  @Transactional(readOnly = true)
  public PublicConnectedBrandsSectionResponse getProjectConnectedBrands(Long projectId, int limit) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    List<PublicBrandConnectedResponse> items = brandCollaborationRepository
        .findPublicByProjectId(projectId, cappedPageable(limit))
        .stream()
        .map(this::toConnectedResponse)
        .toList();

    return PublicConnectedBrandsSectionResponse.builder()
        .projectId(projectId)
        .items(items)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PublicConnectedBrandsSectionResponse getBuilderConnectedBrands(Long builderId, int limit) {
    builderRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(builderId)
        .orElseThrow(() -> new EntityNotFoundException("Builder not found: " + builderId));

    List<PublicBrandConnectedResponse> items = brandCollaborationRepository
        .findPublicByBuilderId(builderId, cappedPageable(limit))
        .stream()
        .map(this::toConnectedResponse)
        .toList();

    return PublicConnectedBrandsSectionResponse.builder()
        .builderId(builderId)
        .items(items)
        .build();
  }

  private Pageable cappedPageable(int limit) {
    int size = Math.min(Math.max(limit, 1), MAX_LIMIT);
    return PageRequest.of(0, size);
  }

  private PublicBrandConnectedResponse toConnectedResponse(BrandCollaborationEntity c) {
    BrandEntity brand = c.getBrand();
    return PublicBrandConnectedResponse.builder()
        .brandId(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .shortDescription(brand.getShortDescription())
        .relationType(c.getRelationType())
        .sourceType(c.getSourceType())
        .verified(Boolean.TRUE.equals(c.getVerified()))
        .featured(Boolean.TRUE.equals(c.getFeatured()))
        .displayOrder(c.getSortOrder() != null ? c.getSortOrder() : 0)
        .build();
  }
}
