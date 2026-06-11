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
  private boolean active;
  private int sortOrder;
}
