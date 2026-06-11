package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.FloorPlanInsightType;
import com.brandPitara.sfs.project.enums.InsightComparisonResult;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "project_floor_plan_insight")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanInsightEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_plan_id", nullable = false)
  private ProjectFloorPlanEntity floorPlan;

  @Enumerated(EnumType.STRING)
  @Column(name = "insight_type", nullable = false, length = 50)
  private FloorPlanInsightType insightType;

  @Column(name = "title", nullable = false, length = 160)
  private String title;

  @Column(name = "summary", length = 500)
  private String summary;

  @Column(name = "detailed_text", columnDefinition = "text")
  private String detailedText;

  // Numeric benchmark comparison fields
  @Column(name = "unit_value", precision = 12, scale = 4)
  private BigDecimal unitValue;

  @Column(name = "benchmark_value", precision = 12, scale = 4)
  private BigDecimal benchmarkValue;

  @Column(name = "unit_label", length = 30)
  private String unitLabel;

  @Column(name = "difference_value", precision = 12, scale = 4)
  private BigDecimal differenceValue;

  @Column(name = "difference_percent", precision = 8, scale = 4)
  private BigDecimal differencePercent;

  // Chart display labels
  @Column(name = "chart_label_this_unit", length = 80)
  private String chartLabelThisUnit;

  @Column(name = "chart_label_average", length = 80)
  private String chartLabelAverage;

  @Enumerated(EnumType.STRING)
  @Column(name = "comparison_result", length = 30)
  private InsightComparisonResult comparisonResult;

  // Legacy simple fields
  @Column(name = "score")
  private Integer score;

  @Column(name = "positive", nullable = false)
  @Builder.Default
  private Boolean positive = true;

  // Visibility and lifecycle
  @Column(name = "public_visible", nullable = false)
  @Builder.Default
  private Boolean publicVisible = true;

  @Column(name = "verified", nullable = false)
  @Builder.Default
  private Boolean verified = false;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "deleted", nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;
}
