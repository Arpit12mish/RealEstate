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
  private List<FloorPlanRoomDimensionResponse> rooms;
  private List<FloorPlanInsightResponse> insights;

  // Visual Analysis block (GAP-027 stabilization) - null when no dashboard
  // user has authored one yet for this floor plan, or when a legacy
  // mediaUrl fails TrustedMediaUrlValidator's read-side check (its mediaUrl
  // is nulled out rather than the whole object being hidden - see
  // ProjectFloorPlanInsightServiceImpl#publicGetDetail).
  private ProjectFloorPlanVisualAnalysisResponse visualAnalysis;

  // True when this floor plan has no authored visualAnalysis yet - purely
  // computed metadata, never a signal that any OTHER field on this response
  // might be synthetic (nothing on this response is ever synthetic; see
  // this field's own javadoc on ProjectFloorPlanVisualAnalysisResponse for
  // the full reasoning). Scoped only to visualAnalysis in this composer -
  // does not consider rooms[]/insights[] richness.
  private boolean demo;
  private String sourceLabel;
}
