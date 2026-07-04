package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.mapper.ProjectMasterPlanMapper;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectMasterPlanServiceImpl implements ProjectMasterPlanService {

  private static final String KEY_PROJECTS = "PROJECTS";
  private static final String KEY_HOME = "HOME";

  private final ProjectRepository projectRepository;
  private final ProjectMasterPlanRepository masterPlanRepository;
  private final ContentVersionService contentVersionService;
  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  @Override
  @Transactional(readOnly = true)
  public ProjectMasterPlanResponse adminGet(Long projectId) {
    assertProjectExists(projectId);
    return masterPlanRepository.findByProjectIdAndDeletedFalse(projectId)
        .map(ProjectMasterPlanMapper::toDashboardResponse)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectMasterPlanResponse publicGet(Long projectId) {
    ProjectEntity project = getProject(projectId);
    projectPublicVisibilityPolicy.assertPubliclyVisible(project, projectId);

    return masterPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
        .map(ProjectMasterPlanMapper::toPublicResponse)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public ProjectMasterPlanResponse dashboardPreviewGet(Long projectId) {
    assertProjectExists(projectId);
    return masterPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
        .map(ProjectMasterPlanMapper::toPublicResponse)
        .orElse(null);
  }

  @Override
  @Transactional
  public ProjectMasterPlanResponse upsert(Long projectId, ProjectMasterPlanUpsertRequest request) {
    ProjectEntity project = getProject(projectId);

    ProjectMasterPlanEntity entity = masterPlanRepository.findByProjectIdAndDeletedFalse(projectId)
        .orElseGet(() -> ProjectMasterPlanEntity.builder()
            .project(project)
            .deleted(false)
            .active(true)
            .verified(false)
            .build());

    applyRequest(entity, request);
    ProjectMasterPlanEntity saved = masterPlanRepository.save(entity);
    bumpContentVersions(project);

    return ProjectMasterPlanMapper.toDashboardResponse(saved);
  }

  @Override
  @Transactional
  public ProjectMasterPlanResponse setActive(Long projectId, boolean active) {
    ProjectMasterPlanEntity entity = masterPlanRepository.findByProjectIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Master plan not found for project: " + projectId));

    entity.setActive(active);
    ProjectMasterPlanEntity saved = masterPlanRepository.save(entity);
    bumpContentVersions(entity.getProject());

    return ProjectMasterPlanMapper.toDashboardResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long projectId) {
    ProjectMasterPlanEntity entity = masterPlanRepository.findByProjectIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Master plan not found for project: " + projectId));

    entity.setDeleted(true);
    entity.setActive(false);
    masterPlanRepository.save(entity);
    bumpContentVersions(entity.getProject());
  }

  private ProjectEntity getProject(Long projectId) {
    if (projectId == null) {
      throw new IllegalArgumentException("Project id is required");
    }

    return projectRepository.findByIdAndDeletedFalse(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
  }

  private void assertProjectExists(Long projectId) {
    getProject(projectId);
  }

  private void applyRequest(ProjectMasterPlanEntity entity, ProjectMasterPlanUpsertRequest request) {
    entity.setTitle(clean(request.getTitle()));
    entity.setSubtitle(clean(request.getSubtitle()));
    entity.setDescription(clean(request.getDescription()));
    entity.setMasterPlanImageUrl(clean(request.getMasterPlanImageUrl()));
    entity.setImageCaption(clean(request.getImageCaption()));
    entity.setImageAltText(clean(request.getImageAltText()));

    entity.setTotalUnits(request.getTotalUnits());
    entity.setTotalTowers(request.getTotalTowers());
    entity.setTotalFloors(request.getTotalFloors());
    entity.setParkAreaValue(request.getParkAreaValue());
    entity.setParkAreaUnit(request.getParkAreaUnit());
    entity.setTotalLandAreaValue(request.getTotalLandAreaValue());
    entity.setTotalLandAreaUnit(request.getTotalLandAreaUnit());
    entity.setOpenSpaceAreaValue(request.getOpenSpaceAreaValue());
    entity.setOpenSpaceAreaUnit(request.getOpenSpaceAreaUnit());
    entity.setGreenAreaValue(request.getGreenAreaValue());
    entity.setGreenAreaUnit(request.getGreenAreaUnit());
    entity.setClubhouseAreaValue(request.getClubhouseAreaValue());
    entity.setClubhouseAreaUnit(request.getClubhouseAreaUnit());
    entity.setAmenityAreaValue(request.getAmenityAreaValue());
    entity.setAmenityAreaUnit(request.getAmenityAreaUnit());
    entity.setRoadWidthValue(request.getRoadWidthValue());
    entity.setRoadWidthUnit(request.getRoadWidthUnit());
    entity.setWaterSource(clean(request.getWaterSource()));
    entity.setParkingType(request.getParkingType());
    entity.setTotalParkingSlots(request.getTotalParkingSlots());
    entity.setVisitorParkingSlots(request.getVisitorParkingSlots());
    entity.setBasementLevels(request.getBasementLevels());
    entity.setEntryExitGates(request.getEntryExitGates());
    entity.setLiftCount(request.getLiftCount());
    entity.setPhaseCount(request.getPhaseCount());
    entity.setCurrentPhase(clean(request.getCurrentPhase()));
    entity.setOpenSpacePercent(request.getOpenSpacePercent());
    entity.setGreenCoveragePercent(request.getGreenCoveragePercent());

    entity.setVastuCompliant(request.getVastuCompliant());
    entity.setGatedCommunity(request.getGatedCommunity());
    entity.setBoundaryWall(request.getBoundaryWall());
    entity.setFireTenderMovement(request.getFireTenderMovement());
    entity.setSewageTreatmentPlant(request.getSewageTreatmentPlant());
    entity.setRainwaterHarvesting(request.getRainwaterHarvesting());
    entity.setPowerBackup(request.getPowerBackup());

    entity.setApprovalStatus(request.getApprovalStatus());
    entity.setVerified(Boolean.TRUE.equals(request.getVerified()));
    entity.setSourceLabel(clean(request.getSourceLabel()));
    entity.setSourceDocumentUrl(clean(request.getSourceDocumentUrl()));
    entity.setLastVerifiedAt(request.getLastVerifiedAt());
    entity.setRemarks(clean(request.getRemarks()));
    entity.setActive(request.getActive() != null ? request.getActive() : true);
    entity.setDeleted(false);
  }

  private void bumpContentVersions(ProjectEntity project) {
    contentVersionService.bump(KEY_PROJECTS);
    if (project != null && Boolean.TRUE.equals(project.getPublished()) && Boolean.TRUE.equals(project.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }
  }

  private String clean(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
