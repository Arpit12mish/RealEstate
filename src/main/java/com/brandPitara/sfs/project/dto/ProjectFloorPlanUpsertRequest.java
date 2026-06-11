package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String title;

  @Size(max = 50)
  private String floorCode;

  @NotBlank
  @Size(max = 500)
  private String imageUrl;

  @Size(max = 50)
  private String carpetArea;

  @Size(max = 50)
  private String exclusiveArea;

  @Size(max = 50)
  private String superArea;

  @Size(max = 80)
  private String unitLabel;

  @Size(max = 1000)
  private String description;

  @Min(0) @Max(9999)
  private Integer sortOrder;
  private Boolean active;

  // --- Floor Plan Intelligence v1 ---

  private UnitConfigurationType unitConfigurationType;

  @Min(0)
  private Long price;

  @DecimalMin("0.0")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal saleableAreaSqft;

  @DecimalMin("0.0")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal carpetAreaSqft;

  @DecimalMin("0.0")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal builtUpAreaSqft;

  @DecimalMin("0.0")
  @Digits(integer = 8, fraction = 2)
  private BigDecimal superAreaSqft;

  @DecimalMin("0.0")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal floorHeightMeters;

  @DecimalMin("0.0") @DecimalMax("100.0")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal carpetEfficiencyPercent;

  @Min(0) @Max(99)
  private Integer bedrooms;

  @Min(0) @Max(99)
  private Integer bathrooms;

  @Min(0) @Max(99)
  private Integer balconies;

  @Size(max = 100)
  private String facing;

  @Size(max = 255)
  private String directionSummary;

  @Size(max = 100)
  private String towerName;

  @Size(max = 100)
  private String floorRange;

  @Size(max = 500)
  private String keyPlanImageUrl;

  private Boolean featured;
}
