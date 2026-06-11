package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanInsightType;
import com.brandPitara.sfs.project.enums.InsightComparisonResult;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorPlanInsightUpsertRequest {

  @NotNull
  private FloorPlanInsightType insightType;

  @NotBlank
  @Size(max = 160)
  private String title;

  @Size(max = 500)
  private String summary;

  @Size(max = 2000)
  private String detailedText;

  // Numeric benchmark comparison
  @Digits(integer = 10, fraction = 4)
  private BigDecimal unitValue;

  @Digits(integer = 10, fraction = 4)
  private BigDecimal benchmarkValue;

  @Size(max = 30)
  private String unitLabel;

  @Digits(integer = 10, fraction = 4)
  private BigDecimal differenceValue;

  @Digits(integer = 6, fraction = 4)
  private BigDecimal differencePercent;

  @Size(max = 80)
  private String chartLabelThisUnit;

  @Size(max = 80)
  private String chartLabelAverage;

  private InsightComparisonResult comparisonResult;

  // Legacy simple score (optional)
  @Min(0) @Max(100)
  private Integer score;

  private Boolean positive;

  // Visibility
  private Boolean publicVisible;
  private Boolean verified;
  private Boolean active;

  @Min(0) @Max(9999)
  private Integer sortOrder;
}
