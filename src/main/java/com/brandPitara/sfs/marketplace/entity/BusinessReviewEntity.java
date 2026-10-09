package com.brandPitara.sfs.marketplace.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "business_review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reviewer_name", nullable = false)
    private String reviewerName;

    @Column(name = "reviewer_location", length = 120)
    private String reviewerLocation;

    @Column(nullable = false)
    private Short rating;

    @Column(name = "review_text", nullable = false, columnDefinition = "text")
    private String reviewText;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    @Builder.Default
    private BusinessReviewStatus moderationStatus = BusinessReviewStatus.PENDING;

    @Column(name = "moderation_note", columnDefinition = "text")
    private String moderationNote;

    @Column(name = "moderated_by_dashboard_user_id")
    private Long moderatedByDashboardUserId;

    @Column(name = "moderated_at")
    private OffsetDateTime moderatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
