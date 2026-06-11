package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "project_floor_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(length = 80)
  private String floorCode;

  @Column(name = "image_url", nullable = false, columnDefinition = "text")
  private String imageUrl;

  @Column(name = "carpet_area", length = 100)
  private String carpetArea;

  @Column(name = "exclusive_area", length = 100)
  private String exclusiveArea;

  @Column(name = "super_area", length = 100)
  private String superArea;

  @Column(name = "unit_label", length = 120)
  private String unitLabel;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  // --- Floor Plan Intelligence v1 ---

  @Enumerated(EnumType.STRING)
  @Column(name = "unit_configuration_type", length = 30)
  private UnitConfigurationType unitConfigurationType;

  @Column(name = "price")
  private Long price;

  @Column(name = "saleable_area_sqft", precision = 10, scale = 2)
  private BigDecimal saleableAreaSqft;

  @Column(name = "carpet_area_sqft", precision = 10, scale = 2)
  private BigDecimal carpetAreaSqft;

  @Column(name = "built_up_area_sqft", precision = 10, scale = 2)
  private BigDecimal builtUpAreaSqft;

  @Column(name = "super_area_sqft", precision = 10, scale = 2)
  private BigDecimal superAreaSqft;

  @Column(name = "floor_height_meters", precision = 5, scale = 2)
  private BigDecimal floorHeightMeters;

  @Column(name = "carpet_efficiency_percent", precision = 5, scale = 2)
  private BigDecimal carpetEfficiencyPercent;

  @Column(name = "bedrooms")
  private Integer bedrooms;

  @Column(name = "bathrooms")
  private Integer bathrooms;

  @Column(name = "balconies")
  private Integer balconies;

  @Column(name = "facing", length = 100)
  private String facing;

  @Column(name = "direction_summary", length = 255)
  private String directionSummary;

  @Column(name = "tower_name", length = 100)
  private String towerName;

  @Column(name = "floor_range", length = 100)
  private String floorRange;

  @Column(name = "key_plan_image_url", columnDefinition = "text")
  private String keyPlanImageUrl;

  @Column(name = "featured", nullable = false)
  @Builder.Default
  private Boolean featured = false;

  @Column(name = "insights_available", nullable = false)
  @Builder.Default
  private Boolean insightsAvailable = false;
}
