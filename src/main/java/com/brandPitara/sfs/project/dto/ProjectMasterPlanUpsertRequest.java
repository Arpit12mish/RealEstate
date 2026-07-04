package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.MasterPlanApprovalStatus;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.project.enums.ParkingType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMasterPlanUpsertRequest {

  @Size(max = 150)
  private String title;

  @Size(max = 300)
  private String subtitle;

  @Size(max = 2000)
  private String description;

  @Size(max = 500)
  private String masterPlanImageUrl;

  @Size(max = 300)
  private String imageCaption;

  @Size(max = 300)
  private String imageAltText;

  @Min(0)
  private Integer totalUnits;

  @Min(0)
  private Integer totalTowers;

  @Min(0)
  private Integer totalFloors;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal parkAreaValue;
  private MasterPlanAreaUnit parkAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal totalLandAreaValue;
  private MasterPlanAreaUnit totalLandAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal openSpaceAreaValue;
  private MasterPlanAreaUnit openSpaceAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal greenAreaValue;
  private MasterPlanAreaUnit greenAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal clubhouseAreaValue;
  private MasterPlanAreaUnit clubhouseAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal amenityAreaValue;
  private MasterPlanAreaUnit amenityAreaUnit;

  @DecimalMin("0.0")
  @Digits(integer = 6, fraction = 2)
  private BigDecimal roadWidthValue;
  private MasterPlanAreaUnit roadWidthUnit;

  @Size(max = 120)
  private String waterSource;

  private ParkingType parkingType;

  @Min(0)
  private Integer totalParkingSlots;

  @Min(0)
  private Integer visitorParkingSlots;

  @Min(0)
  private Integer basementLevels;

  @Min(0)
  private Integer entryExitGates;

  @Min(0)
  private Integer liftCount;

  @Min(0)
  private Integer phaseCount;

  @Size(max = 80)
  private String currentPhase;

  @DecimalMin("0.0")
  @DecimalMax("100.0")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal openSpacePercent;

  @DecimalMin("0.0")
  @DecimalMax("100.0")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal greenCoveragePercent;

  private Boolean vastuCompliant;
  private Boolean gatedCommunity;
  private Boolean boundaryWall;
  private Boolean fireTenderMovement;
  private Boolean sewageTreatmentPlant;
  private Boolean rainwaterHarvesting;
  private Boolean powerBackup;

  private MasterPlanApprovalStatus approvalStatus;
  private Boolean verified;

  @Size(max = 180)
  private String sourceLabel;

  @Size(max = 500)
  private String sourceDocumentUrl;

  private OffsetDateTime lastVerifiedAt;

  @Size(max = 1000)
  private String remarks;

  private Boolean active;
}
