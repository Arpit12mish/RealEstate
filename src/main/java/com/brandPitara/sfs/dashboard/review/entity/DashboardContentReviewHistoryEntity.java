package com.brandPitara.sfs.dashboard.review.entity;

import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_content_review_history",
        indexes = {
                @Index(name = "idx_dashboard_review_history_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_dashboard_review_history_action_by", columnList = "action_by"),
                @Index(name = "idx_dashboard_review_history_action_type", columnList = "action_type"),
                @Index(name = "idx_dashboard_review_history_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardContentReviewHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Example:
     * PROJECT, PROJECT_MEDIA, PROJECT_METER, BUILDER, APP_CONTENT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 80)
    private ReviewEntityType entityType;

    /**
     * ID of the target content/entity being reviewed.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Nullable because CREATED/UPDATED may not always have old status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private ReviewStatus oldStatus;

    /**
     * Nullable because some audit actions may not change main status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private ReviewStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 80)
    private ReviewActionType actionType;

    @Column(columnDefinition = "text")
    private String remarks;

    /**
     * Nullable with ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by")
    private DashboardUserEntity actionBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public static DashboardContentReviewHistoryEntity of(
            ReviewEntityType entityType,
            Long entityId,
            ReviewStatus oldStatus,
            ReviewStatus newStatus,
            ReviewActionType actionType,
            String remarks,
            DashboardUserEntity actionBy
    ) {
        return DashboardContentReviewHistoryEntity.builder()
                .entityType(entityType)
                .entityId(entityId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actionType(actionType)
                .remarks(remarks)
                .actionBy(actionBy)
                .build();
    }
}