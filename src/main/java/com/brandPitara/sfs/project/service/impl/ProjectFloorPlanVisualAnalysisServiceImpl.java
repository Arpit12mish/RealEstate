package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.media.validator.TrustedMediaUrlValidator;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisUpsertRequest;
import com.brandPitara.sfs.project.dto.VisualAnalysisTagUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisTagEntity;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanVisualAnalysisMapper;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanVisualAnalysisRepository;
import com.brandPitara.sfs.project.service.ProjectFloorPlanVisualAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFloorPlanVisualAnalysisServiceImpl implements ProjectFloorPlanVisualAnalysisService {

  private static final String DEFAULT_TITLE = "Visual analysis of every important factor";

  private final ProjectFloorPlanRepository floorPlanRepository;
  private final ProjectFloorPlanVisualAnalysisRepository visualAnalysisRepository;
  private final ContentVersionService contentVersionService;
  private final TrustedMediaUrlValidator trustedMediaUrlValidator;

  @Override
  @Transactional(readOnly = true)
  public ProjectFloorPlanVisualAnalysisResponse get(Long projectId, Long floorPlanId) {
    resolveFloorPlan(projectId, floorPlanId);
    return visualAnalysisRepository.findByFloorPlanIdAndDeletedFalse(floorPlanId)
        .map(ProjectFloorPlanVisualAnalysisMapper::toResponse)
        .orElse(null);
  }

  @Override
  @Transactional
  public ProjectFloorPlanVisualAnalysisResponse upsert(Long projectId, Long floorPlanId, ProjectFloorPlanVisualAnalysisUpsertRequest request) {
    ProjectFloorPlanEntity floorPlan = resolveFloorPlan(projectId, floorPlanId);

    // GAP-028: format/host validation happens before anything is persisted -
    // an invalid mediaUrl must never reach the entity, let alone the public
    // read endpoint.
    trustedMediaUrlValidator.validate(request.getMediaUrl());
    trustedMediaUrlValidator.validateMediaTypeCompatibility(request.getMediaType(), request.getMediaUrl());

    ProjectFloorPlanVisualAnalysisEntity entity = visualAnalysisRepository
        .findByFloorPlanIdAndDeletedFalse(floorPlanId)
        .orElseGet(() -> ProjectFloorPlanVisualAnalysisEntity.builder()
            .floorPlan(floorPlan)
            .title(DEFAULT_TITLE)
            .build());

    if (request.getTitle() != null) entity.setTitle(clean(request.getTitle()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getMediaType() != null) entity.setMediaType(request.getMediaType());
    if (request.getMediaUrl() != null) entity.setMediaUrl(clean(request.getMediaUrl()));
    if (request.getActive() != null) entity.setActive(request.getActive());
    if (request.getTags() != null) replaceTags(entity, request.getTags());

    if (entity.getMediaType() == null) entity.setMediaType(FloorPlanVisualMediaType.IMAGE);
    if (!StringUtils.hasText(entity.getTitle())) entity.setTitle(DEFAULT_TITLE);

    ProjectFloorPlanVisualAnalysisEntity saved = visualAnalysisRepository.save(entity);
    contentVersionService.bump("PROJECTS");
    return ProjectFloorPlanVisualAnalysisMapper.toResponse(saved);
  }

  private void replaceTags(ProjectFloorPlanVisualAnalysisEntity entity, List<VisualAnalysisTagUpsertRequest> tagRequests) {
    entity.getTags().clear();
    List<ProjectFloorPlanVisualAnalysisTagEntity> tags = new ArrayList<>();
    for (int i = 0; i < tagRequests.size(); i++) {
      VisualAnalysisTagUpsertRequest tr = tagRequests.get(i);
      tags.add(ProjectFloorPlanVisualAnalysisTagEntity.builder()
          .visualAnalysis(entity)
          .label(tr.getLabel().trim())
          .color(StringUtils.hasText(tr.getColor()) ? tr.getColor() : "#3B7DDD")
          .sortOrder(tr.getSortOrder() != null ? tr.getSortOrder() : i)
          .active(true)
          .build());
    }
    entity.getTags().addAll(tags);
  }

  private ProjectFloorPlanEntity resolveFloorPlan(Long projectId, Long floorPlanId) {
    ProjectFloorPlanEntity fp = floorPlanRepository.findByIdAndDeletedFalse(floorPlanId)
        .orElseThrow(() -> new NotFoundException("Floor plan not found: " + floorPlanId));
    if (!fp.getProject().getId().equals(projectId)) {
      throw new NotFoundException("Floor plan not found for project: " + floorPlanId);
    }
    return fp;
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
