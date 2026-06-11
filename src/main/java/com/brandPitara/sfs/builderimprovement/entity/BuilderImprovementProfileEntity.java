package com.brandPitara.sfs.builderimprovement.entity;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementEvidenceStatus;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementOverallStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "builder_improvement_profile",
        indexes = {
                @Index(name = "idx_builder_improvement_profile_builder", columnList = "builder_id"),
                @Index(name = "idx_builder_improvement_profile_project", columnList = "project_id"),
                @Index(name = "idx_builder_improvement_profile_review_status", columnList = "review_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementProfileEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Master builder reference. Do not duplicate builder name/logo/city here.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "builder_id", nullable = false)
    private BuilderEntity builder;

    /**
     * Nullable.
     * null = builder-level journey.
     * not null = project-specific journey.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @Builder.Default
    @Column(name = "badge_text", nullable = false, length = 120)
    private String badgeText = "Commitment 2026";

    @Column(name = "hero_title", columnDefinition = "text")
    private String heroTitle;

    @Column(name = "hero_subtitle", columnDefinition = "text")
    private String heroSubtitle;

    @Column(name = "hero_image_url", columnDefinition = "text")
    private String heroImageUrl;

    @Builder.Default
    @Column(name = "verified_by_text", length = 180)
    private String verifiedByText = "Verified by SFS Trust Engine";

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "builder_response_title", length = 180)
    private String builderResponseTitle;

    @Column(name = "builder_response_person_name", length = 150)
    private String builderResponsePersonName;

    @Column(name = "builder_response_person_designation", length = 150)
    private String builderResponsePersonDesignation;

    @Column(name = "builder_response_text", columnDefinition = "text")
    private String builderResponseText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 40)
    private BuilderImprovementOverallStatus overallStatus = BuilderImprovementOverallStatus.UNDER_REVIEW;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_status", nullable = false, length = 40)
    private BuilderImprovementEvidenceStatus evidenceStatus = BuilderImprovementEvidenceStatus.EVIDENCE_PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 40)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "published", nullable = false)
    private Boolean published = false;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_by_dashboard_user_id")
    private Long createdByDashboardUserId;

    @Column(name = "updated_by_dashboard_user_id")
    private Long updatedByDashboardUserId;

    @Column(name = "submitted_by_dashboard_user_id")
    private Long submittedByDashboardUserId;

    @Column(name = "reviewed_by_dashboard_user_id")
    private Long reviewedByDashboardUserId;

    @Column(name = "published_by_dashboard_user_id")
    private Long publishedByDashboardUserId;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "review_remarks", columnDefinition = "text")
    private String reviewRemarks;

    public boolean isProjectSpecific() {
        return project != null;
    }

    public boolean isPubliclyVisible() {
        return Boolean.TRUE.equals(active)
                && Boolean.TRUE.equals(published)
                && !Boolean.TRUE.equals(deleted)
                && reviewStatus == ReviewStatus.APPROVED;
    }
}