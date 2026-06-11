package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.FloorPlanInsightResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanInsightEntity;

public class ProjectFloorPlanInsightMapper {

  public static FloorPlanInsightResponse toResponse(ProjectFloorPlanInsightEntity e) {
    return FloorPlanInsightResponse.builder()
        .id(e.getId())
        .floorPlanId(e.getFloorPlan() != null ? e.getFloorPlan().getId() : null)
        .insightType(e.getInsightType())
        .insightTypeLabel(e.getInsightType() != null ? e.getInsightType().toLabel() : null)
        .title(e.getTitle())
        .summary(e.getSummary())
        .detailedText(e.getDetailedText())
        .unitValue(e.getUnitValue())
        .benchmarkValue(e.getBenchmarkValue())
        .unitLabel(e.getUnitLabel())
        .differenceValue(e.getDifferenceValue())
        .differencePercent(e.getDifferencePercent())
        .chartLabelThisUnit(e.getChartLabelThisUnit())
        .chartLabelAverage(e.getChartLabelAverage())
        .comparisonResult(e.getComparisonResult())
        .comparisonResultLabel(e.getComparisonResult() != null ? e.getComparisonResult().toLabel() : null)
        .score(e.getScore())
        .positive(Boolean.TRUE.equals(e.getPositive()))
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .active(Boolean.TRUE.equals(e.getActive()))
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }
}
