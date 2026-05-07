package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "project",
    indexes = {
        @Index(name = "idx_project_builder_id", columnList = "builder_id"),
        @Index(name = "idx_project_city_id",    columnList = "city_id"),
        @Index(name = "idx_project_public",     columnList = "published,active,deleted,review_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "builder_id", nullable = false)
  private BuilderEntity builder;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(length = 220, unique = true)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(columnDefinition = "text")
  private String addressLine;

  private Double latitude;
  private Double longitude;

  private Long priceMin;
  private Long priceMax;

  private LocalDate possessionDate;

  @Column(length = 60)
  private String reraNumber;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private ProjectStatus status;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "project_property_types", joinColumns = @JoinColumn(name = "project_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", length = 30)
  @BatchSize(size = 25)
  @Builder.Default
  private Set<PropertyType> propertyTypes = new HashSet<>();

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean published = false;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  /**
   * Dashboard review status.
   *
   * Public APIs should only expose data when:
   * published = true
   * active = true
   * deleted = false
   * reviewStatus = APPROVED
   *
   * We will update repository/service public checks in the next hardening step.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "review_status", nullable = false, length = 40)
  @Builder.Default
  private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

  @Column(name = "created_by_dashboard_user_id")
  private Long createdByDashboardUserId;

  @Column(name = "submitted_by_dashboard_user_id")
  private Long submittedByDashboardUserId;

  @Column(name = "submitted_at")
  private OffsetDateTime submittedAt;

  @Column(name = "reviewed_by_dashboard_user_id")
  private Long reviewedByDashboardUserId;

  @Column(name = "reviewed_at")
  private OffsetDateTime reviewedAt;

  @Column(name = "review_remarks", columnDefinition = "text")
  private String reviewRemarks;

  public boolean isDeletedProject() {
    return Boolean.TRUE.equals(deleted);
  }

  public boolean isApprovedForPublishing() {
    return reviewStatus == ReviewStatus.APPROVED;
  }
}