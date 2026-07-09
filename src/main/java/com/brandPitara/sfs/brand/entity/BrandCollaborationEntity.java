package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Connects a brand to exactly one of project/builder/company/business/companyProject.
 * Which FK is populated must match {@link #targetType}; this is enforced at
 * the database level by chk_brand_collaboration_single_target (see V118/V130).
 * Architects and interior designers are both {@code company} rows, reached
 * via {@link BrandCollaborationTargetType#COMPANY}.
 */
@Entity
@Table(name = "brand_collaboration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandCollaborationEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 20)
  private BrandCollaborationTargetType targetType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private ProjectEntity project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "builder_id")
  private BuilderEntity builder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private CompanyEntity company;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_id")
  private BusinessEntity business;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_project_id")
  private CompanyProjectEntity companyProject;

  @Column(length = 50)
  private String role;

  @Enumerated(EnumType.STRING)
  @Column(name = "relation_type", length = 50)
  private BrandRelationType relationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", length = 50)
  private BrandSourceType sourceType;

  @Column(name = "usage_category", length = 150)
  private String usageCategory;

  @Column(length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  @Builder.Default
  private Boolean verified = false;

  @Column(name = "public_visible", nullable = false)
  @Builder.Default
  private Boolean publicVisible = false;

  @Column(nullable = false)
  @Builder.Default
  private Boolean featured = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
