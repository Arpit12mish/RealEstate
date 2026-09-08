package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;

import java.math.BigDecimal;

public class ProjectFloorPlanRoomDimensionMapper {

  public static FloorPlanRoomDimensionResponse toResponse(ProjectFloorPlanRoomDimensionEntity e) {
    boolean hasComparisonData = e.getAverageAreaSqft() != null;

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
        .averageAreaSqft(e.getAverageAreaSqft())
        .comparisonContextLabel(e.getComparisonContextLabel())
        .differencePercent(e.getDifferencePercent())
        .comparisonLabel(hasComparisonData ? toComparisonLabel(e.getDifferencePercent()) : null)
        .summary(hasComparisonData ? e.getComparisonSummary() : null)
        .comparisonVerified(Boolean.TRUE.equals(e.getComparisonVerified()))
        .hasComparisonData(hasComparisonData)
        .active(Boolean.TRUE.equals(e.getActive()))
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }

  private static String toComparisonLabel(BigDecimal differencePercent) {
    if (differencePercent == null) return null;
    String stripped = differencePercent.stripTrailingZeros().toPlainString();
    String sign = differencePercent.signum() >= 0 ? "+" : "";
    return sign + stripped + "% from avg";
  }
}
