package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMapper;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

  private final ProjectRepository projectRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectMediaRepository projectMediaRepository;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager em;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public ProjectResponse create(Long builderId, ProjectUpsertRequest request) {
    BuilderEntity builderRef = em.getReference(BuilderEntity.class, builderId);

    ProjectEntity entity = ProjectEntity.builder()
        .builder(builderRef)
        .name(clean(request.getName()))
        .slug(clean(request.getSlug()))
        .description(clean(request.getDescription()))
        .city(resolveCity(request.getCityId()))
        .addressLine(clean(request.getAddressLine()))
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .priceMin(request.getPriceMin())
        .priceMax(request.getPriceMax())
        .possessionDate(request.getPossessionDate())
        .reraNumber(clean(request.getReraNumber()))
        .status(request.getStatus())
        .propertyTypes(request.getPropertyTypes() != null ? request.getPropertyTypes() : entityDefaultTypes())
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .published(false)
        .deleted(false)
        .build();

    ProjectEntity saved = projectRepository.save(entity);
    contentVersionService.bump(KEY_PROJECTS);
    return ProjectMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectResponse update(Long projectId, ProjectUpsertRequest request) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getSlug() != null) entity.setSlug(clean(request.getSlug()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));

    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));

    if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());

    if (request.getPriceMin() != null) entity.setPriceMin(request.getPriceMin());
    if (request.getPriceMax() != null) entity.setPriceMax(request.getPriceMax());

    if (request.getPossessionDate() != null) entity.setPossessionDate(request.getPossessionDate());
    if (request.getReraNumber() != null) entity.setReraNumber(clean(request.getReraNumber()));

    if (request.getStatus() != null) entity.setStatus(request.getStatus());
    if (request.getPropertyTypes() != null) entity.setPropertyTypes(request.getPropertyTypes());

    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());

    ProjectEntity saved = projectRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(saved.getPublished()) && Boolean.TRUE.equals(saved.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    return ProjectMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectResponse setPublished(Long projectId, boolean published) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    entity.setPublished(published);
    ProjectEntity saved = projectRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
    return ProjectMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectResponse setActive(Long projectId, boolean active) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    entity.setActive(active);
    ProjectEntity saved = projectRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
    return ProjectMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long projectId) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    entity.setDeleted(true);
    entity.setPublished(false);
    projectRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectResponse adminGet(Long projectId) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    return ProjectMapper.toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectResponse> adminList(Long builderId, Pageable pageable) {
    Page<ProjectEntity> page = (builderId == null)
        ? projectRepository.findByDeletedFalse(pageable)
        : projectRepository.findByBuilderIdAndDeletedFalse(builderId, pageable);
    return page.map(ProjectMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectResponse> publicListByBuilder(Long builderId, Pageable pageable) {

    Page<ProjectEntity> page = projectRepository
        .findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalse(builderId, pageable);

    var projectIds = page.getContent().stream()
        .map(ProjectEntity::getId)
        .toList();

    java.util.Map<Long, java.util.List<com.brandPitara.sfs.project.entity.ProjectMediaEntity>> mediaMap =
        java.util.Collections.emptyMap();

    if (!projectIds.isEmpty()) {
      var mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);
      mediaMap = mediaList.stream().collect(
          java.util.stream.Collectors.groupingBy(m -> m.getProject().getId())
      );
    }

    final var finalMediaMap = mediaMap;

    // return page.map(p -> ProjectMapper.toResponse(p, finalMediaMap.get(p.getId())));
    return page.map(p -> ProjectMapper.toResponse(
    p,
    finalMediaMap.getOrDefault(p.getId(), java.util.List.of())
));

  }


  @Override
  @Transactional(readOnly = true)
  public ProjectResponse publicGet(Long projectId) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    if (!Boolean.TRUE.equals(entity.getPublished()) || !Boolean.TRUE.equals(entity.getActive())) {
      throw new NotFoundException("Project not found: " + projectId);
    }
    // ✅ Fetch media for this project
    var media = projectMediaRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(projectId);

    // ✅ Enriched response (cover/brochure flags)
    return ProjectMapper.toResponse(entity, media);
  }

  // helpers
  private CityEntity resolveCity(Long cityId) {
    if (cityId == null) return null;
    return em.getReference(CityEntity.class, cityId);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private java.util.Set<com.brandPitara.sfs.project.enums.PropertyType> entityDefaultTypes() {
    return new java.util.HashSet<>();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectResponse> publicFeatured(Long builderId, Pageable pageable) {

    Page<ProjectEntity> page = (builderId == null)
        ? projectRepository.findByPublishedTrueAndActiveTrueAndDeletedFalse(pageable)
        : projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalse(builderId, pageable);

    var projectIds = page.getContent().stream().map(ProjectEntity::getId).toList();

    java.util.Map<Long, java.util.List<ProjectMediaEntity>> mediaMap = java.util.Collections.emptyMap();

    if (!projectIds.isEmpty()) {
      var mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);
      mediaMap = mediaList.stream()
          .collect(java.util.stream.Collectors.groupingBy(m -> m.getProject().getId()));
    }

    final var finalMediaMap = mediaMap;
    return page.map(p -> ProjectMapper.toResponse(p, finalMediaMap.get(p.getId())));
  }

}
