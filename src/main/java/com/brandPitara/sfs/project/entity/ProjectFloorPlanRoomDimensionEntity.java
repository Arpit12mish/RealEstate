package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.FloorPlanRoomType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "project_floor_plan_room_dimension")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanRoomDimensionEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_plan_id", nullable = false)
  private ProjectFloorPlanEntity floorPlan;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 50)
  private FloorPlanRoomType roomType;

  @Column(name = "label", length = 100)
  private String label;

  @Column(name = "length_ft", precision = 8, scale = 2)
  private BigDecimal lengthFt;

  @Column(name = "width_ft", precision = 8, scale = 2)
  private BigDecimal widthFt;

  @Column(name = "area_sqft", precision = 10, scale = 2)
  private BigDecimal areaSqft;

  // Pre-formatted display string, e.g. "12.1 ft x 10.6 ft"
  @Column(name = "dimension_text", length = 100)
  private String dimensionText;

  @Column(name = "icon_key", length = 60)
  private String iconKey;

  @Column(name = "notes", length = 255)
  private String notes;

  // Space Comparison fields — authored directly on the room row rather than
  // joined through project_floor_plan_insight, since insight_type coverage
  // does not span every FloorPlanRoomType and there is no FK between the two.
  @Column(name = "average_area_sqft", precision = 10, scale = 2)
  private BigDecimal averageAreaSqft;

  @Column(name = "comparison_context_label", length = 120)
  private String comparisonContextLabel;

  @Column(name = "difference_percent", precision = 6, scale = 2)
  private BigDecimal differencePercent;

  @Column(name = "comparison_summary")
  private String comparisonSummary;

  @Column(name = "comparison_verified", nullable = false)
  @Builder.Default
  private Boolean comparisonVerified = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "deleted", nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
