package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanResponse {
  private Long id;
  private Long projectId;
  private String title;
  private String floorCode;
  private String imageUrl;
  private String carpetArea;
  private String exclusiveArea;
  private String superArea;
  private String unitLabel;
  private String description;
  private int sortOrder;
  private boolean active;
  private boolean featured;
  private boolean insightsAvailable;

  // Structured intelligence fields
  private UnitConfigurationType unitConfigurationType;
  private String unitConfigurationTypeLabel;
  private Long price;
  private BigDecimal saleableAreaSqft;
  private BigDecimal carpetAreaSqft;
  private BigDecimal builtUpAreaSqft;
  private BigDecimal superAreaSqft;
  private BigDecimal floorHeightMeters;
  private BigDecimal carpetEfficiencyPercent;
  private Integer bedrooms;
  private Integer bathrooms;
  private Integer balconies;
  private String facing;
  private String directionSummary;
  private String towerName;
  private String floorRange;
  private String keyPlanImageUrl;
}
