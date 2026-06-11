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
}
