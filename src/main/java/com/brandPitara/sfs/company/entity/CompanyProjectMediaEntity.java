package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_project_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectMediaEntity extends BaseEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_project_id", nullable = false)
  private CompanyProjectEntity companyProject;

  @Column(name = "media_url", nullable = false, columnDefinition = "text")
  private String mediaUrl;

  @Column(name = "media_type", nullable = false, length = 20)
  @Builder.Default private String mediaType = "IMAGE";

  @Column(name = "category_key", nullable = false, length = 80)
  private String categoryKey;
  @Column(name = "category_label", length = 120) private String categoryLabel;
  @Column(length = 160) private String title;
  @Column(columnDefinition = "text") private String description;
  @Column(name = "metric_label", length = 80) private String metricLabel;
  @Column(name = "metric_value", length = 120) private String metricValue;
  @Column(name = "sort_order", nullable = false) @Builder.Default private Integer sortOrder = 0;
  @Column(name = "public_visible", nullable = false) @Builder.Default private Boolean publicVisible = true;
  @Column(nullable = false) @Builder.Default private Boolean active = true;
  @Column(nullable = false) @Builder.Default private Boolean deleted = false;
}
