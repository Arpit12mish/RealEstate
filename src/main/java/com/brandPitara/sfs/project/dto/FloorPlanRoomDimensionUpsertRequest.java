package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorPlanRoomDimensionUpsertRequest {

  @NotNull
  private FloorPlanRoomType roomType;

  @Size(max = 100)
  private String label;

  @DecimalMin("0.0")
  @Digits(integer = 6, fraction = 2)
  private BigDecimal lengthFt;

  @DecimalMin("0.0")
  @Digits(integer = 6, fraction = 2)
  private BigDecimal widthFt;

  @DecimalMin("0.0")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal areaSqft;

  // Pre-formatted display string, e.g. "12.1 ft x 10.6 ft"
  @Size(max = 100)
  private String dimensionText;

  @Size(max = 60)
  private String iconKey;

  @Size(max = 255)
  private String notes;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean active;
}
