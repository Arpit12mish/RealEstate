package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_media",
    indexes = {
        @Index(name = "idx_proj_media_project_id",     columnList = "project_id"),
        @Index(name = "idx_proj_media_active_lookup",  columnList = "project_id,active,deleted,sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMediaEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ProjectMediaType mediaType;

  @Column(nullable = false, columnDefinition = "text")
  private String url;

  @Column(length = 200)
  private String caption;

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
