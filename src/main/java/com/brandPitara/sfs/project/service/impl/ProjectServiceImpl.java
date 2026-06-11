package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectPatchRequest;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMapper;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectDetailComposer;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

  private final BuilderRepository builderRepository;
  private final ProjectRepository projectRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectMediaRepository projectMediaRepository;
  private final ProjectFavoriteService projectFavoriteService;
  private final ProjectDetailComposer projectDetailComposer;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager em;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public ProjectResponse create(Long builderId, ProjectUpsertRequest request) {
    validateSlugAvailableForCreate(clean(request.getSlug()));

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
        .monthlyEmiMin(request.getMonthlyEmiMin())
        .monthlyEmiMax(request.getMonthlyEmiMax())
        .averagePricePerSqft(request.getAveragePricePerSqft())
        .startDate(request.getStartDate())
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

    applyUpdate(entity, request, projectId);

    return saveDashboardProject(entity);
  }

  @Override
  @Transactional
  public ProjectResponse patch(Long projectId, ProjectPatchRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Project request body is required");
    }

    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    validatePatchAgainstCurrentValues(entity, request);
    applyPatch(entity, request, projectId);

    return saveDashboardProject(entity);
  }

  private ProjectResponse saveDashboardProject(ProjectEntity entity) {

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

    if (published && entity.getReviewStatus() != ReviewStatus.APPROVED) {
      throw new IllegalStateException(
          "Project must be APPROVED before it can be published. Current review status: " +
              (entity.getReviewStatus() != null ? entity.getReviewStatus() : ReviewStatus.DRAFT)
      );
    }

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
  public ProjectResponse reassignBuilder(Long projectId, Long builderId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    BuilderEntity builder = builderRepository.findByIdAndDeletedFalse(builderId)
        .orElseThrow(() -> new NotFoundException("Builder not found: " + builderId));

    project.setBuilder(builder);
    ProjectEntity saved = projectRepository.save(project);

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
  public Page<ProjectResponse> dashboardList(Long builderId, ReviewStatus reviewStatus, Pageable pageable) {
    Page<ProjectEntity> page;

    if (builderId == null && reviewStatus == null) {
      page = projectRepository.findByDeletedFalse(pageable);
    } else if (builderId != null && reviewStatus == null) {
      page = projectRepository.findByBuilderIdAndDeletedFalse(builderId, pageable);
    } else if (builderId == null) {
      page = projectRepository.findByDeletedFalseAndReviewStatus(reviewStatus, pageable);
    } else {
      page = projectRepository.findByBuilderIdAndDeletedFalseAndReviewStatus(builderId, reviewStatus, pageable);
    }

    return page.map(ProjectMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectPublicResponse> publicListByBuilder(Long builderId, Pageable pageable) {
    Page<ProjectEntity> page = projectRepository
        .findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
            builderId,
            ReviewStatus.APPROVED,
            pageable
        );

    List<Long> projectIds = page.getContent().stream()
        .map(ProjectEntity::getId)
        .toList();

    Map<Long, List<ProjectMediaEntity>> mediaMap;

    if (!projectIds.isEmpty()) {
      var mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);
      mediaMap = mediaList.stream()
          .collect(Collectors.groupingBy(m -> m.getProject().getId()));
    } else {
      mediaMap = Collections.emptyMap();
    }

    List<ProjectPublicResponse> responses = page.getContent().stream()
        .map(p -> ProjectMapper.toPublicResponse(
            p,
            mediaMap.getOrDefault(p.getId(), List.of())
        ))
        .toList();

    projectFavoriteService.enrichPublicProjects(responses);

    return new PageImpl<>(responses, pageable, page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectPublicResponse publicGet(Long projectId) {
    ProjectEntity entity = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    projectPublicVisibilityPolicy.assertPubliclyVisible(entity, projectId);

    List<ProjectMediaEntity> media =
        projectMediaRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(projectId);

    ProjectPublicResponse response = projectDetailComposer.composePublic(entity, media);
    projectFavoriteService.enrichPublicProject(response);

    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectPublicResponse> publicFeatured(Long builderId, Pageable pageable) {
    Page<ProjectEntity> page = (builderId == null)
        ? projectRepository.findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
            ReviewStatus.APPROVED,
            pageable
        )
        : projectRepository.findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
            builderId,
            ReviewStatus.APPROVED,
            pageable
        );

    List<Long> projectIds = page.getContent().stream()
        .map(ProjectEntity::getId)
        .toList();

    Map<Long, List<ProjectMediaEntity>> mediaMap;

    if (!projectIds.isEmpty()) {
      var mediaList = projectMediaRepository.findActiveByProjectIds(projectIds);
      mediaMap = mediaList.stream()
          .collect(Collectors.groupingBy(m -> m.getProject().getId()));
    } else {
      mediaMap = Collections.emptyMap();
    }

    List<ProjectPublicResponse> responses = page.getContent().stream()
        .map(p -> ProjectMapper.toPublicResponse(
            p,
            mediaMap.getOrDefault(p.getId(), List.of())
        ))
        .toList();

    projectFavoriteService.enrichPublicProjects(responses);

    return new PageImpl<>(responses, pageable, page.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProjectPublicResponse> publicBrowse(List<UnitConfigurationType> unitConfigurations, Long cityId, Pageable pageable) {
    Page<ProjectEntity> page;

    boolean hasUnitFilter = unitConfigurations != null && !unitConfigurations.isEmpty();

    if (hasUnitFilter) {
      page = projectRepository.browsePublicByUnitConfiguration(
          ReviewStatus.APPROVED, cityId, unitConfigurations, pageable);
    } else {
      page = cityId != null
          ? projectRepository.findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
              cityId, ReviewStatus.APPROVED, pageable)
          : projectRepository.findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
              ReviewStatus.APPROVED, pageable);
    }

    List<Long> projectIds = page.getContent().stream().map(ProjectEntity::getId).toList();
    Map<Long, List<ProjectMediaEntity>> mediaMap = projectIds.isEmpty()
        ? Collections.emptyMap()
        : projectMediaRepository.findActiveByProjectIds(projectIds).stream()
            .collect(Collectors.groupingBy(m -> m.getProject().getId()));

    List<ProjectPublicResponse> responses = page.getContent().stream()
        .map(p -> ProjectMapper.toPublicResponse(p, mediaMap.getOrDefault(p.getId(), List.of())))
        .toList();

    projectFavoriteService.enrichPublicProjects(responses);
    return new PageImpl<>(responses, pageable, page.getTotalElements());
  }

  private void applyUpdate(ProjectEntity entity, ProjectUpsertRequest request, Long projectId) {
    if (request.getSlug() != null) {
      validateSlugAvailableForUpdate(clean(request.getSlug()), projectId);
    }

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getSlug() != null) entity.setSlug(clean(request.getSlug()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));

    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));

    if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());

    if (request.getPriceMin() != null) entity.setPriceMin(request.getPriceMin());
    if (request.getPriceMax() != null) entity.setPriceMax(request.getPriceMax());
    if (request.getMonthlyEmiMin() != null) entity.setMonthlyEmiMin(request.getMonthlyEmiMin());
    if (request.getMonthlyEmiMax() != null) entity.setMonthlyEmiMax(request.getMonthlyEmiMax());
    if (request.getAveragePricePerSqft() != null) entity.setAveragePricePerSqft(request.getAveragePricePerSqft());

    if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
    if (request.getPossessionDate() != null) entity.setPossessionDate(request.getPossessionDate());
    if (request.getReraNumber() != null) entity.setReraNumber(clean(request.getReraNumber()));

    if (request.getStatus() != null) entity.setStatus(request.getStatus());
    if (request.getPropertyTypes() != null) entity.setPropertyTypes(request.getPropertyTypes());

    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());
  }

  private void applyPatch(ProjectEntity entity, ProjectPatchRequest request, Long projectId) {
    if (request.getSlug() != null) {
      validateSlugAvailableForUpdate(clean(request.getSlug()), projectId);
    }

    if (request.getName() != null) {
      if (!StringUtils.hasText(request.getName())) {
        throw new IllegalArgumentException("Project name is required");
      }
      entity.setName(clean(request.getName()));
    }
    if (request.getSlug() != null) entity.setSlug(clean(request.getSlug()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));

    if (request.getCityId() != null) entity.setCity(resolveCity(request.getCityId()));
    if (request.getAddressLine() != null) entity.setAddressLine(clean(request.getAddressLine()));

    if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
    if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());

    if (request.getPriceMin() != null) entity.setPriceMin(request.getPriceMin());
    if (request.getPriceMax() != null) entity.setPriceMax(request.getPriceMax());
    if (request.getMonthlyEmiMin() != null) entity.setMonthlyEmiMin(request.getMonthlyEmiMin());
    if (request.getMonthlyEmiMax() != null) entity.setMonthlyEmiMax(request.getMonthlyEmiMax());
    if (request.getAveragePricePerSqft() != null) entity.setAveragePricePerSqft(request.getAveragePricePerSqft());

    if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
    if (request.getPossessionDate() != null) entity.setPossessionDate(request.getPossessionDate());
    if (request.getReraNumber() != null) entity.setReraNumber(clean(request.getReraNumber()));

    if (request.getStatus() != null) entity.setStatus(request.getStatus());
    if (request.getPropertyTypes() != null) entity.setPropertyTypes(request.getPropertyTypes());

    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());
  }

  private void validatePatchAgainstCurrentValues(ProjectEntity entity, ProjectPatchRequest request) {
    Long priceMin = request.getPriceMin() != null ? request.getPriceMin() : entity.getPriceMin();
    Long priceMax = request.getPriceMax() != null ? request.getPriceMax() : entity.getPriceMax();
    if (priceMin != null && priceMax != null && priceMin > priceMax) {
      throw new IllegalArgumentException("priceMin (" + priceMin + ") must not exceed priceMax (" + priceMax + ")");
    }

    Long emiMin = request.getMonthlyEmiMin() != null ? request.getMonthlyEmiMin() : entity.getMonthlyEmiMin();
    Long emiMax = request.getMonthlyEmiMax() != null ? request.getMonthlyEmiMax() : entity.getMonthlyEmiMax();
    if (emiMin != null && emiMax != null && emiMin > emiMax) {
      throw new IllegalArgumentException("monthlyEmiMin (" + emiMin + ") must not exceed monthlyEmiMax (" + emiMax + ")");
    }

    Double latitude = request.getLatitude() != null ? request.getLatitude() : entity.getLatitude();
    Double longitude = request.getLongitude() != null ? request.getLongitude() : entity.getLongitude();
    if ((latitude != null) != (longitude != null)) {
      throw new IllegalArgumentException("latitude and longitude must both be provided together or both omitted");
    }
  }

  private CityEntity resolveCity(Long cityId) {
    if (cityId == null) {
      return null;
    }

    return em.getReference(CityEntity.class, cityId);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }

    return s.trim();
  }

  private void validateSlugAvailableForCreate(String slug) {
    if (!StringUtils.hasText(slug)) {
      return;
    }

    projectRepository.findBySlug(slug).ifPresent(existing -> {
      throw slugConflict(slug, existing.getId());
    });
  }

  private void validateSlugAvailableForUpdate(String slug, Long projectId) {
    if (!StringUtils.hasText(slug)) {
      return;
    }

    projectRepository.findBySlugAndIdNot(slug, projectId).ifPresent(existing -> {
      throw slugConflict(slug, existing.getId());
    });
  }

  private ResponseStatusException slugConflict(String slug, Long existingProjectId) {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Project slug already exists: " + slug + " (projectId=" + existingProjectId + ")"
    );
  }

  private java.util.Set<com.brandPitara.sfs.project.enums.PropertyType> entityDefaultTypes() {
    return new java.util.HashSet<>();
  }
}
