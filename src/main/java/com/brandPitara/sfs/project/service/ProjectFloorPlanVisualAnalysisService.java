package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisUpsertRequest;

public interface ProjectFloorPlanVisualAnalysisService {

  // Dashboard: existing row or null if none authored yet.
  ProjectFloorPlanVisualAnalysisResponse get(Long projectId, Long floorPlanId);

  // Dashboard: get-or-create semantics - creates the singleton row on first call.
  ProjectFloorPlanVisualAnalysisResponse upsert(Long projectId, Long floorPlanId, ProjectFloorPlanVisualAnalysisUpsertRequest request);
}
