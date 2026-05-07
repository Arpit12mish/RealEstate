package com.brandPitara.sfs.dashboard.review.entity;

import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_field_review_issues",
        indexes = {
                @Index(name = "idx_dashboard_field_review_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_dashboard_field_review_field_key", columnList = "field_key"),
                @Index(name = "idx_dashboard_field_review_status", columnList = "status"),
                @Index(name = "idx_dashboard_field_review_active", columnList = "active"),
                @Index(name = "idx_dashboard_field_review_marked_by", columnList = "marked_by"),
                @Index(name = "idx_dashboard_field_review_fixed_by", columnList = "fixed_by")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFieldReviewIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Example:
     * PROJECT, PROJECT_MEDIA, PROJECT_FLOOR_PLAN, PROJECT_METER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 80)
    private ReviewEntityType entityType;

    /**
     * Target entity ID.
     * Example: projectId, mediaId, floorPlanId.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Stable backend key.
     * Example:
     * reraNumber, priceMin, possessionDate, latitude, longitude.
     */
    @Column(name = "field_key", nullable = false, length = 120)
    private String fieldKey;

    /**
     * Display label for dashboard UI.
     * Example:
     * RERA Number, Minimum Price, Possession Date.
     */
    @Column(name = "field_label", length = 180)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FieldReviewStatus status;

    @Column(columnDefinition = "text")
    private String remarks;

    /**
     * ADMIN / REVIEWER who marked the issue.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marked_by", nullable = false)
    private DashboardUserEntity markedBy;

    /**
     * DATA_ENTRY / ADMIN who fixed the issue.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_by")
    private DashboardUserEntity fixedBy;

    @Column(name = "marked_at", nullable = false, updatable = false)
    private OffsetDateTime markedAt;

    @Column(name = "fixed_at")
    private OffsetDateTime fixedAt;

    /**
     * true = issue is currently open.
     * false = issue is resolved/closed.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        this.markedAt = OffsetDateTime.now();

        if (this.active == null) {
            this.active = true;
        }

        if (this.fieldKey != null) {
            this.fieldKey = this.fieldKey.trim();
        }

        if (this.fieldLabel != null) {
            this.fieldLabel = this.fieldLabel.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.fieldKey != null) {
            this.fieldKey = this.fieldKey.trim();
        }

        if (this.fieldLabel != null) {
            this.fieldLabel = this.fieldLabel.trim();
        }
    }

    public boolean isActiveIssue() {
        return Boolean.TRUE.equals(active)
                && status != null
                && status.isActiveIssue();
    }

    public boolean isFixed() {
        return status == FieldReviewStatus.FIXED;
    }

    public void markFixed(DashboardUserEntity fixedByUser) {
        this.status = FieldReviewStatus.FIXED;
        this.fixedBy = fixedByUser;
        this.fixedAt = OffsetDateTime.now();
        this.active = false;
    }

    public void reopen(FieldReviewStatus newStatus, String remarks, DashboardUserEntity markedByUser) {
        if (newStatus == FieldReviewStatus.FIXED) {
            throw new IllegalArgumentException("Cannot reopen issue with FIXED status");
        }

        this.status = newStatus;
        this.remarks = remarks;
        this.markedBy = markedByUser;
        this.fixedBy = null;
        this.fixedAt = null;
        this.active = true;
        this.markedAt = OffsetDateTime.now();
    }
}