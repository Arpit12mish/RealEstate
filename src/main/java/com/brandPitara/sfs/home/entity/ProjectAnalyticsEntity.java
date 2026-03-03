package com.brandPitara.sfs.home.entity;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_analytics")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectAnalyticsEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "builder_id")
  private BuilderEntity builder;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "image_url", nullable = false, columnDefinition = "text")
  private String imageUrl;

  @Column(length = 255)
  private String caption;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
