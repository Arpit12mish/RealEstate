package com.brandPitara.sfs.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_floor_plan_visual_analysis_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanVisualAnalysisTagEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "visual_analysis_id", nullable = false)
  private ProjectFloorPlanVisualAnalysisEntity visualAnalysis;

  @Column(name = "label", nullable = false, length = 60)
  private String label;

  @Column(name = "color", nullable = false, length = 9)
  @Builder.Default
  private String color = "#3B7DDD";

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;
}
