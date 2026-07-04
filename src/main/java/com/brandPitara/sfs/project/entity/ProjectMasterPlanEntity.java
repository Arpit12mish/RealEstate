package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.MasterPlanApprovalStatus;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.project.enums.ParkingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_master_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMasterPlanEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @Column(length = 150)
  private String title;

  @Column(length = 300)
  private String subtitle;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "master_plan_image_url", columnDefinition = "text")
  private String masterPlanImageUrl;

  @Column(name = "image_caption", length = 300)
  private String imageCaption;

  @Column(name = "image_alt_text", length = 300)
  private String imageAltText;

  @Column(name = "total_units")
  private Integer totalUnits;

  @Column(name = "total_towers")
  private Integer totalTowers;

  @Column(name = "total_floors")
  private Integer totalFloors;

  @Column(name = "park_area_value", precision = 12, scale = 2)
  private BigDecimal parkAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "park_area_unit", length = 20)
  private MasterPlanAreaUnit parkAreaUnit;

  @Column(name = "total_land_area_value", precision = 12, scale = 2)
  private BigDecimal totalLandAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "total_land_area_unit", length = 20)
  private MasterPlanAreaUnit totalLandAreaUnit;

  @Column(name = "open_space_area_value", precision = 12, scale = 2)
  private BigDecimal openSpaceAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "open_space_area_unit", length = 20)
  private MasterPlanAreaUnit openSpaceAreaUnit;

  @Column(name = "green_area_value", precision = 12, scale = 2)
  private BigDecimal greenAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "green_area_unit", length = 20)
  private MasterPlanAreaUnit greenAreaUnit;

  @Column(name = "clubhouse_area_value", precision = 12, scale = 2)
  private BigDecimal clubhouseAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "clubhouse_area_unit", length = 20)
  private MasterPlanAreaUnit clubhouseAreaUnit;

  @Column(name = "amenity_area_value", precision = 12, scale = 2)
  private BigDecimal amenityAreaValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "amenity_area_unit", length = 20)
  private MasterPlanAreaUnit amenityAreaUnit;

  @Column(name = "road_width_value", precision = 8, scale = 2)
  private BigDecimal roadWidthValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "road_width_unit", length = 20)
  private MasterPlanAreaUnit roadWidthUnit;

  @Column(name = "water_source", length = 120)
  private String waterSource;

  @Enumerated(EnumType.STRING)
  @Column(name = "parking_type", length = 30)
  private ParkingType parkingType;

  @Column(name = "total_parking_slots")
  private Integer totalParkingSlots;

  @Column(name = "visitor_parking_slots")
  private Integer visitorParkingSlots;

  @Column(name = "basement_levels")
  private Integer basementLevels;

  @Column(name = "entry_exit_gates")
  private Integer entryExitGates;

  @Column(name = "lift_count")
  private Integer liftCount;

  @Column(name = "phase_count")
  private Integer phaseCount;

  @Column(name = "current_phase", length = 80)
  private String currentPhase;

  @Column(name = "open_space_percent", precision = 5, scale = 2)
  private BigDecimal openSpacePercent;

  @Column(name = "green_coverage_percent", precision = 5, scale = 2)
  private BigDecimal greenCoveragePercent;

  @Column(name = "vastu_compliant")
  private Boolean vastuCompliant;

  @Column(name = "gated_community")
  private Boolean gatedCommunity;

  @Column(name = "boundary_wall")
  private Boolean boundaryWall;

  @Column(name = "fire_tender_movement")
  private Boolean fireTenderMovement;

  @Column(name = "sewage_treatment_plant")
  private Boolean sewageTreatmentPlant;

  @Column(name = "rainwater_harvesting")
  private Boolean rainwaterHarvesting;

  @Column(name = "power_backup")
  private Boolean powerBackup;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", length = 30)
  private MasterPlanApprovalStatus approvalStatus;

  @Column(nullable = false)
  @Builder.Default
  private Boolean verified = false;

  @Column(name = "source_label", length = 180)
  private String sourceLabel;

  @Column(name = "source_document_url", columnDefinition = "text")
  private String sourceDocumentUrl;

  @Column(name = "last_verified_at")
  private OffsetDateTime lastVerifiedAt;

  @Column(columnDefinition = "text")
  private String remarks;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
