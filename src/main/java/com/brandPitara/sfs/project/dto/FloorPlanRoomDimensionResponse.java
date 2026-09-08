package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorPlanRoomDimensionResponse {
  private Long id;
  private Long floorPlanId;
  private FloorPlanRoomType roomType;
  private String roomTypeLabel;
  private String label;
  private BigDecimal lengthFt;
  private BigDecimal widthFt;
  private BigDecimal areaSqft;
  private String dimensionText;
  private String iconKey;
  private String notes;

  // Space Comparison — populated only when a dashboard user has authored
  // comparison data for this room; hasComparisonData tells the client
  // whether to render the badge/summary block or omit it gracefully.
  private BigDecimal averageAreaSqft;
  private String comparisonContextLabel;
  private BigDecimal differencePercent;
  private String comparisonLabel;
  private String summary;
  private boolean comparisonVerified;
  private boolean hasComparisonData;

  private boolean active;
  private int sortOrder;
}
