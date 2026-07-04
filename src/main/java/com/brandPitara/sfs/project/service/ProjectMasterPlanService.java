package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanUpsertRequest;

public interface ProjectMasterPlanService {

  ProjectMasterPlanResponse adminGet(Long projectId);

  ProjectMasterPlanResponse publicGet(Long projectId);

  ProjectMasterPlanResponse dashboardPreviewGet(Long projectId);

  ProjectMasterPlanResponse upsert(Long projectId, ProjectMasterPlanUpsertRequest request);

  ProjectMasterPlanResponse setActive(Long projectId, boolean active);

  void softDelete(Long projectId);
}
