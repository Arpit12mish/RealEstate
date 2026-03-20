package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
}