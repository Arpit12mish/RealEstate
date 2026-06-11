package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;

public class ProjectFloorPlanRoomDimensionMapper {

  public static FloorPlanRoomDimensionResponse toResponse(ProjectFloorPlanRoomDimensionEntity e) {
    return FloorPlanRoomDimensionResponse.builder()
        .id(e.getId())
        .floorPlanId(e.getFloorPlan() != null ? e.getFloorPlan().getId() : null)
        .roomType(e.getRoomType())
        .roomTypeLabel(e.getRoomType() != null ? e.getRoomType().toLabel() : null)
        .label(e.getLabel())
        .lengthFt(e.getLengthFt())
        .widthFt(e.getWidthFt())
        .areaSqft(e.getAreaSqft())
        .dimensionText(e.getDimensionText())
        .iconKey(e.getIconKey())
        .notes(e.getNotes())
        .active(Boolean.TRUE.equals(e.getActive()))
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }
}
