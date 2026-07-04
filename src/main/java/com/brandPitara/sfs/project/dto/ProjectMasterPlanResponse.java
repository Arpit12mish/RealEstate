package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.MasterPlanApprovalStatus;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.project.enums.ParkingType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectMasterPlanResponse {
  private Long id;
  private Long projectId;

  private String title;
  private String subtitle;
  private String description;
  private String imageUrl;
  private String imageCaption;
  private String imageAltText;
  private Boolean expandable;

  private Boolean verified;
  private String sourceLabel;
  private OffsetDateTime lastVerifiedAt;
  private List<ProjectMasterPlanStatResponse> stats;

  // Dashboard-only management fields. Public mapper leaves these null.
  private String sourceDocumentUrl;
  private String remarks;
  private Boolean active;
  private MasterPlanApprovalStatus approvalStatus;

  // Dashboard form fields.
  private Integer totalUnits;
  private Integer totalTowers;
  private Integer totalFloors;
  private BigDecimal parkAreaValue;
  private MasterPlanAreaUnit parkAreaUnit;
  private BigDecimal totalLandAreaValue;
  private MasterPlanAreaUnit totalLandAreaUnit;
  private BigDecimal openSpaceAreaValue;
  private MasterPlanAreaUnit openSpaceAreaUnit;
  private BigDecimal greenAreaValue;
  private MasterPlanAreaUnit greenAreaUnit;
  private BigDecimal clubhouseAreaValue;
  private MasterPlanAreaUnit clubhouseAreaUnit;
  private BigDecimal amenityAreaValue;
  private MasterPlanAreaUnit amenityAreaUnit;
  private BigDecimal roadWidthValue;
  private MasterPlanAreaUnit roadWidthUnit;
  private String waterSource;
  private ParkingType parkingType;
  private Integer totalParkingSlots;
  private Integer visitorParkingSlots;
  private Integer basementLevels;
  private Integer entryExitGates;
  private Integer liftCount;
  private Integer phaseCount;
  private String currentPhase;
  private BigDecimal openSpacePercent;
  private BigDecimal greenCoveragePercent;
  private Boolean vastuCompliant;
  private Boolean gatedCommunity;
  private Boolean boundaryWall;
  private Boolean fireTenderMovement;
  private Boolean sewageTreatmentPlant;
  private Boolean rainwaterHarvesting;
  private Boolean powerBackup;
}
