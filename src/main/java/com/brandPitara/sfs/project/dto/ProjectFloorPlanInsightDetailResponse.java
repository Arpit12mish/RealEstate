package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanInsightDetailResponse {
  private Long floorPlanId;
  private Long projectId;
  private String title;
  private String imageUrl;
  private String unitLabel;
  private UnitConfigurationType unitConfigurationType;
  private String unitConfigurationTypeLabel;
  private Long price;
  private BigDecimal carpetAreaSqft;
  private BigDecimal saleableAreaSqft;
  private BigDecimal superAreaSqft;
  private BigDecimal carpetEfficiencyPercent;
  private Integer bedrooms;
  private Integer bathrooms;
  private Integer balconies;
  private String facing;
  private String directionSummary;
  private String towerName;
  private String floorRange;
  private String keyPlanImageUrl;

  // rooms[] already carries Space Comparison data per room (see
  // FloorPlanRoomDimensionResponse#averageAreaSqft/differencePercent/summary/
  // hasComparisonData) - no separate spaceComparison wrapper is needed since
  // that would just duplicate the same rows in a second shape.
  private List<FloorPlanRoomDimensionResponse> rooms;
  private List<FloorPlanInsightResponse> insights;

  // Visual Analysis block - null when no dashboard user has authored one yet.
  private ProjectFloorPlanVisualAnalysisResponse visualAnalysis;

  // True when neither visualAnalysis nor any room in rooms[] has real authored
  // content yet, so the client can label content as sample/demo instead of
  // silently rendering an unconditional local fallback.
  private boolean demo;
  private String sourceLabel;
}
