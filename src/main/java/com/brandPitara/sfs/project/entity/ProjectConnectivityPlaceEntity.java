package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_connectivity_place")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "connectivity_id")
  private ProjectConnectivityEntity connectivity;

  @Column(name = "place_name", nullable = false, length = 180)
  private String placeName;

  @Enumerated(EnumType.STRING)
  @Column(name = "place_type", nullable = false, length = 60)
  private ProjectConnectivityType placeType;

  @Column(name = "distance_label", length = 80)
  private String distanceLabel;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}