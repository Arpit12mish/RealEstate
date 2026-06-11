package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanInsightType;
import com.brandPitara.sfs.project.enums.InsightComparisonResult;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorPlanInsightResponse {
  private Long id;
  private Long floorPlanId;
  private FloorPlanInsightType insightType;
  private String insightTypeLabel;
  private String title;
  private String summary;
  private String detailedText;

  // Numeric benchmark fields
  private BigDecimal unitValue;
  private BigDecimal benchmarkValue;
  private String unitLabel;
  private BigDecimal differenceValue;
  private BigDecimal differencePercent;

  // Chart labels
  private String chartLabelThisUnit;
  private String chartLabelAverage;

  private InsightComparisonResult comparisonResult;
  private String comparisonResultLabel;

  // Legacy
  private Integer score;
  private boolean positive;

  // Visibility
  private boolean publicVisible;
  private boolean verified;
  private boolean active;
  private int sortOrder;
}
