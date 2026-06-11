package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;

public class ProjectFloorPlanMapper {

  public static ProjectFloorPlanResponse toResponse(ProjectFloorPlanEntity e) {
    return ProjectFloorPlanResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .title(e.getTitle())
        .floorCode(e.getFloorCode())
        .imageUrl(e.getImageUrl())
        .carpetArea(e.getCarpetArea())
        .exclusiveArea(e.getExclusiveArea())
        .superArea(e.getSuperArea())
        .unitLabel(e.getUnitLabel())
        .description(e.getDescription())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .featured(Boolean.TRUE.equals(e.getFeatured()))
        .insightsAvailable(Boolean.TRUE.equals(e.getInsightsAvailable()))
        .unitConfigurationType(e.getUnitConfigurationType())
        .unitConfigurationTypeLabel(e.getUnitConfigurationType() != null ? e.getUnitConfigurationType().toLabel() : null)
        .price(e.getPrice())
        .saleableAreaSqft(e.getSaleableAreaSqft())
        .carpetAreaSqft(e.getCarpetAreaSqft())
        .builtUpAreaSqft(e.getBuiltUpAreaSqft())
        .superAreaSqft(e.getSuperAreaSqft())
        .floorHeightMeters(e.getFloorHeightMeters())
        .carpetEfficiencyPercent(e.getCarpetEfficiencyPercent())
        .bedrooms(e.getBedrooms())
        .bathrooms(e.getBathrooms())
        .balconies(e.getBalconies())
        .facing(e.getFacing())
        .directionSummary(e.getDirectionSummary())
        .towerName(e.getTowerName())
        .floorRange(e.getFloorRange())
        .keyPlanImageUrl(e.getKeyPlanImageUrl())
        .build();
  }
}
