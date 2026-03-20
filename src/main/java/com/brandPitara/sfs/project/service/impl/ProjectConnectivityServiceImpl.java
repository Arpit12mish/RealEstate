package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.dto.*;
import com.brandPitara.sfs.project.entity.*;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityMapper;
import com.brandPitara.sfs.project.repository.*;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectConnectivityServiceImpl implements ProjectConnectivityService {

  private final ProjectRepository projectRepository;
  private final ProjectConnectivityRepository connectivityRepository;
  private final ProjectConnectivityPlaceRepository placeRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional(readOnly = true)
  public ProjectConnectivityResponse adminGet(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId).orElse(null);
    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);

    return ProjectConnectivityMapper.toResponse(connectivity, project.getId(), places);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectConnectivityResponse publicGet(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    if (!Boolean.TRUE.equals(project.getPublished()) || !Boolean.TRUE.equals(project.getActive())) {
      throw new EntityNotFoundException("Project not found: " + projectId);
    }

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndActiveTrueAndDeletedFalse(projectId).orElse(null);
    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);

    return ProjectConnectivityMapper.toResponse(connectivity, project.getId(), places);
  }

  @Override
  @Transactional
  public ProjectConnectivityResponse upsertOverview(Long projectId, ProjectConnectivityUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity entity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId)
        .orElseGet(() -> ProjectConnectivityEntity.builder()
            .project(project)
            .build());

    entity.setTitle(clean(request.getTitle()));
    entity.setSubtitle(clean(request.getSubtitle()));
    entity.setMapImageUrl(clean(request.getMapImageUrl()));
    entity.setActive(request.getActive() != null ? request.getActive() : true);
    if (entity.getDeleted() == null) entity.setDeleted(false);

    connectivityRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    List<ProjectConnectivityPlaceEntity> places = placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);
    return ProjectConnectivityMapper.toResponse(entity, projectId, places);
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceResponse addPlace(Long projectId, ProjectConnectivityPlaceUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectConnectivityEntity connectivity = connectivityRepository.findByProjectIdAndDeletedFalse(projectId).orElse(null);

    ProjectConnectivityPlaceEntity entity = ProjectConnectivityPlaceEntity.builder()
        .project(project)
        .connectivity(connectivity)
        .placeName(cleanRequired(request.getPlaceName()))
        .placeType(request.getPlaceType())
        .distanceLabel(clean(request.getDistanceLabel()))
        .imageUrl(clean(request.getImageUrl()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectConnectivityMapper.toPlaceResponse(saved);
  }

  @Override
  @Transactional
  public ProjectConnectivityPlaceResponse updatePlace(Long projectId, Long placeId, ProjectConnectivityPlaceUpsertRequest request) {
    ProjectConnectivityPlaceEntity entity = placeRepository.findByIdAndProjectIdAndDeletedFalse(placeId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Connectivity place not found: " + placeId));

    if (StringUtils.hasText(request.getPlaceName())) entity.setPlaceName(cleanRequired(request.getPlaceName()));
    if (request.getPlaceType() != null) entity.setPlaceType(request.getPlaceType());
    if (request.getDistanceLabel() != null) entity.setDistanceLabel(clean(request.getDistanceLabel()));
    if (request.getImageUrl() != null) entity.setImageUrl(clean(request.getImageUrl()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    ProjectConnectivityPlaceEntity saved = placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectConnectivityMapper.toPlaceResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectConnectivityPlaceResponse> adminListPlaces(Long projectId) {
    return placeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectConnectivityMapper::toPlaceResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDeletePlace(Long projectId, Long placeId) {
    ProjectConnectivityPlaceEntity entity = placeRepository.findByIdAndProjectIdAndDeletedFalse(placeId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Connectivity place not found: " + placeId));

    entity.setDeleted(true);
    placeRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private String cleanRequired(String s) {
    return s.trim();
  }
}