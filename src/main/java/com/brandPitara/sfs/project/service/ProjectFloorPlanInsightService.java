package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.FloorPlanInsightResponse;
import com.brandPitara.sfs.project.dto.FloorPlanInsightUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;

import java.util.List;

public interface ProjectFloorPlanInsightService {
  FloorPlanInsightResponse create(Long projectId, Long floorPlanId, FloorPlanInsightUpsertRequest request);
  FloorPlanInsightResponse update(Long projectId, Long floorPlanId, Long insightId, FloorPlanInsightUpsertRequest request);
  void delete(Long projectId, Long floorPlanId, Long insightId);
  List<FloorPlanInsightResponse> list(Long projectId, Long floorPlanId);
  ProjectFloorPlanInsightDetailResponse publicGetDetail(Long projectId, Long floorPlanId);
}
