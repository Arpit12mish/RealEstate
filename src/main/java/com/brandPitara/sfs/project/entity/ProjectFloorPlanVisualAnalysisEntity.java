package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.FloorPlanVisualMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

// One row per floor plan (enforced by a unique constraint on floor_plan_id).
// Service layer implements get-or-create semantics on top of this uniqueness.
@Entity
@Table(name = "project_floor_plan_visual_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFloorPlanVisualAnalysisEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_plan_id", nullable = false)
  private ProjectFloorPlanEntity floorPlan;

  @Column(name = "title", nullable = false, length = 160)
  private String title;

  @Column(name = "description", length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 20)
  @Builder.Default
  private FloorPlanVisualMediaType mediaType = FloorPlanVisualMediaType.IMAGE;

  @Column(name = "media_url")
  private String mediaUrl;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "deleted", nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  @OneToMany(mappedBy = "visualAnalysis", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC, id ASC")
  @Builder.Default
  private List<ProjectFloorPlanVisualAnalysisTagEntity> tags = new ArrayList<>();
}
