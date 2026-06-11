package com.brandPitara.sfs.publicreview.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.publicreview.enums.SfsReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "project_review",
    indexes = {
        @Index(name = "idx_project_review_project_id", columnList = "project_id"),
        @Index(name = "idx_project_review_project_status", columnList = "project_id,verification_status"),
        @Index(name = "idx_project_review_display_status", columnList = "display_status"),
        @Index(name = "idx_project_review_user_id", columnList = "user_id"),
        @Index(name = "idx_project_review_user_project", columnList = "user_id,project_id"),
        @Index(name = "idx_project_review_user_status", columnList = "user_id,verification_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "reviewer_name", length = 255)
    private String reviewerName;

    @Column(name = "reviewer_phone_hash", length = 255)
    private String reviewerPhoneHash;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "headline", length = 300)
    private String headline;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 60)
    @Builder.Default
    private SfsReviewSourceType sourceType = SfsReviewSourceType.USER_SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 40)
    @Builder.Default
    private SfsReviewVerificationStatus verificationStatus = SfsReviewVerificationStatus.PENDING;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "sentiment", length = 30)
    private String sentiment;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "display_status", nullable = false, length = 30)
    @Builder.Default
    private String displayStatus = "INTERNAL_ONLY";

    @Column(name = "reviewed_by_dashboard_user_id")
    private Long reviewedByDashboardUserId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_phone_hash", length = 255)
    private String userPhoneHash;

    @Column(name = "submitted_by_user", nullable = false)
    @Builder.Default
    private Boolean submittedByUser = false;
}
