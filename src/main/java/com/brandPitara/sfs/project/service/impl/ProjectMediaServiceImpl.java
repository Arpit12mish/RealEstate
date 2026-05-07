package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.mapper.ProjectMediaMapper;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMediaServiceImpl implements ProjectMediaService {

  private final ProjectRepository projectRepository;
  private final ProjectMediaRepository mediaRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public ProjectMediaResponse addMedia(Long projectId, ProjectMediaUpsertRequest request) {
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    ProjectMediaEntity entity = ProjectMediaEntity.builder()
        .project(project)
        .mediaType(request.getMediaType())
        .url(clean(request.getUrl()))
        .caption(clean(request.getCaption()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    ProjectMediaEntity saved = mediaRepository.save(entity);

    contentVersionService.bump(KEY_PROJECTS);
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    return ProjectMediaMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectMediaResponse> adminList(Long projectId) {
    return mediaRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAscIdDesc(projectId)
        .stream().map(ProjectMediaMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectMediaResponse> publicList(Long projectId) {
    // Ensure project is public
    ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    return mediaRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(projectId)
        .stream().map(ProjectMediaMapper::toResponse).toList();
  }

  @Override
  @Transactional
  public ProjectMediaResponse updateMedia(Long projectId, Long mediaId, ProjectMediaUpsertRequest request) {
    ProjectMediaEntity media = mediaRepository.findByIdAndProjectIdAndDeletedFalse(mediaId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));

    if (request.getMediaType() != null) media.setMediaType(request.getMediaType());
    if (request.getUrl() != null)       media.setUrl(clean(request.getUrl()));
    if (request.getCaption() != null)   media.setCaption(clean(request.getCaption()));
    if (request.getSortOrder() != null) media.setSortOrder(request.getSortOrder());
    if (request.getActive() != null)    media.setActive(request.getActive());

    ProjectMediaEntity saved = mediaRepository.save(media);

    contentVersionService.bump(KEY_PROJECTS);
    ProjectEntity project = saved.getProject();
    if (Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
    return ProjectMediaMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long projectId, Long mediaId) {
    ProjectMediaEntity media = mediaRepository.findByIdAndProjectIdAndDeletedFalse(mediaId, projectId)
        .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));

    media.setDeleted(true);
    mediaRepository.save(media);

    contentVersionService.bump(KEY_PROJECTS);
    contentVersionService.bump(KEY_HOME);
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
