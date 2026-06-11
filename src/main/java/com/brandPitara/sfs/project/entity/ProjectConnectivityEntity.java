package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_connectivity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false, unique = true)
  private ProjectEntity project;

  @Column(length = 160)
  private String title;

  @Column(length = 260)
  private String subtitle;

  @Column(name = "map_image_url", columnDefinition = "text")
  private String mapImageUrl;

  @Column(columnDefinition = "text")
  private String summary;

  @Column(name = "default_radius_meters")
  private Integer defaultRadiusMeters;

  @Column(name = "search_enabled", nullable = false)
  @Builder.Default
  private Boolean searchEnabled = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
