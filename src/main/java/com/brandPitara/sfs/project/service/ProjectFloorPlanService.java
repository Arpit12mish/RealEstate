package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanUpsertRequest;

import java.util.List;

public interface ProjectFloorPlanService {
  ProjectFloorPlanResponse create(Long projectId, ProjectFloorPlanUpsertRequest request);
  ProjectFloorPlanResponse update(Long projectId, Long floorPlanId, ProjectFloorPlanUpsertRequest request);
  ProjectFloorPlanResponse setActive(Long projectId, Long floorPlanId, boolean active);
  void softDelete(Long projectId, Long floorPlanId);

  List<ProjectFloorPlanResponse> adminList(Long projectId);
  List<ProjectFloorPlanResponse> publicList(Long projectId);
  List<ProjectFloorPlanResponse> dashboardPreviewList(Long projectId);
}
