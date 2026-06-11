package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionUpsertRequest;

import java.util.List;

public interface ProjectFloorPlanRoomDimensionService {
  FloorPlanRoomDimensionResponse create(Long projectId, Long floorPlanId, FloorPlanRoomDimensionUpsertRequest request);
  FloorPlanRoomDimensionResponse update(Long projectId, Long floorPlanId, Long roomId, FloorPlanRoomDimensionUpsertRequest request);
  void delete(Long projectId, Long floorPlanId, Long roomId);
  List<FloorPlanRoomDimensionResponse> list(Long projectId, Long floorPlanId);
}
