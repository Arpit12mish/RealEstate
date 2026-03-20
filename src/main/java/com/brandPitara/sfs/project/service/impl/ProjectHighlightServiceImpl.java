package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.dto.ProjectHighlightUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectHighlightEntity;
import com.brandPitara.sfs.project.mapper.ProjectHighlightMapper;
import com.brandPitara.sfs.project.repository.ProjectHighlightRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectHighlightService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectHighlightServiceImpl implements ProjectHighlightService {

  private final ProjectRepository projectRepository;
  private final ProjectHighlightRepository highlightRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public ProjectHighlightResponse addHighlight(Long projectId, ProjectHighlightUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectHighlightEntity entity = ProjectHighlightEntity.builder()
        .project(project)
        .title(cleanRequired(request.getTitle()))
        .subtitle(clean(request.getSubtitle()))
        .iconKey(clean(request.getIconKey()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    ProjectHighlightEntity saved = highlightRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectHighlightMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProjectHighlightResponse updateHighlight(Long projectId, Long highlightId, ProjectHighlightUpsertRequest request) {
    ProjectHighlightEntity entity = highlightRepository.findByIdAndProjectIdAndDeletedFalse(highlightId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Highlight not found: " + highlightId));

    if (StringUtils.hasText(request.getTitle())) entity.setTitle(cleanRequired(request.getTitle()));
    if (request.getSubtitle() != null) entity.setSubtitle(clean(request.getSubtitle()));
    if (request.getIconKey() != null) entity.setIconKey(clean(request.getIconKey()));
    if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    ProjectHighlightEntity saved = highlightRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(entity.getProject().getPublished()) && Boolean.TRUE.equals(entity.getProject().getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return ProjectHighlightMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectHighlightResponse> adminList(Long projectId) {
    return highlightRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectHighlightMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectHighlightResponse> publicList(Long projectId) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    if (!Boolean.TRUE.equals(project.getPublished()) || !Boolean.TRUE.equals(project.getActive())) {
      throw new EntityNotFoundException("Project not found: " + projectId);
    }

    return highlightRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
        .stream()
        .map(ProjectHighlightMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDelete(Long projectId, Long highlightId) {
    ProjectHighlightEntity entity = highlightRepository.findByIdAndProjectIdAndDeletedFalse(highlightId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Highlight not found: " + highlightId));

    entity.setDeleted(true);
    highlightRepository.save(entity);

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