package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanMapper;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFloorPlanServiceImpl implements ProjectFloorPlanService {

  private final ProjectRepository projectRepository;
  private final ProjectFloorPlanRepository projectFloorPlanRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public ProjectFloorPlanResponse create(Long projectId, ProjectFloorPlanUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    ProjectFloorPlanEntity entity = ProjectFloorPlanEntity.builder()
        .project(project)
        .title(cleanRequired(request.getTitle()))
        .floorCode(clean(request.getFloorCode()))
        .imageUrl(cleanRequired(request.getImageUrl()))
        .carpetArea(clean(request.getCarpetArea()))
        .exclusiveArea(clean(request.getExclusiveArea()))
        .superArea(clean(request.getSuperArea()))
        .unitLabel(clean(request.getUnitLabel()))
        .description(clean(request.getDescription()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    ProjectFloorPlanEntity saved = projectFloorPlanRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectFloorPlanMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectFloorPlanResponse update(Long floorPlanId, ProjectFloorPlanUpsertRequest request) {
    ProjectFloorPlanEntity entity = projectFloorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));

    if (request.getTitle() != null) entity.setTitle(cleanRequired(request.getTitle()));
    if (request.getFloorCode() != null) entity.setFloorCode(clean(request.getFloorCode()));
    if (request.getImageUrl() != null) entity.setImageUrl(cleanRequired(request.getImageUrl()));
    if (request.getCarpetArea() != null) entity.setCarpetArea(clean(request.getCarpetArea()));
    if (request.getExclusiveArea() != null) entity.setExclusiveArea(clean(request.getExclusiveArea()));
    if (request.getSuperArea() != null) entity.setSuperArea(clean(request.getSuperArea()));
    if (request.getUnitLabel() != null) entity.setUnitLabel(clean(request.getUnitLabel()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    ProjectFloorPlanEntity saved = projectFloorPlanRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectFloorPlanMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectFloorPlanResponse setActive(Long floorPlanId, boolean active) {
    ProjectFloorPlanEntity entity = projectFloorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));

    entity.setActive(active);
    ProjectFloorPlanEntity saved = projectFloorPlanRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectFloorPlanMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long floorPlanId) {
    ProjectFloorPlanEntity entity = projectFloorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));

    entity.setDeleted(true);
    entity.setActive(false);
    projectFloorPlanRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectFloorPlanResponse> adminList(Long projectId) {
    return projectFloorPlanRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectFloorPlanMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectFloorPlanResponse> publicList(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

    if (!Boolean.TRUE.equals(project.getPublished()) || !Boolean.TRUE.equals(project.getActive())) {
      throw new NotFoundException("Project not found: " + projectId);
    }

    return projectFloorPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectFloorPlanMapper::toResponse)
        .toList();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private String cleanRequired(String s) {
    String cleaned = clean(s);
    if (!StringUtils.hasText(cleaned)) {
      throw new IllegalArgumentException("Required value is missing");
    }
    return cleaned;
  }
}