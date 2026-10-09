package com.brandPitara.sfs.marketplace.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus;
import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * "This store works with this worker" (Connected Workers). A link may also carry a store
 * recommendation, which is public only once a dashboard reviewer has VERIFIED it.
 */
@Entity
@Table(name = "business_worker_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessWorkerLinkEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderProfileEntity provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkerLinkStatus status = WorkerLinkStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", nullable = false, length = 20)
    @Builder.Default
    private WorkerRecommendationStatus recommendationStatus = WorkerRecommendationStatus.NONE;

    @Column(name = "recommendation_note", length = 300)
    private String recommendationNote;

    @Column(name = "recommendation_reviewed_by")
    private Long recommendationReviewedBy;

    @Column(name = "recommendation_reviewed_at")
    private OffsetDateTime recommendationReviewedAt;

    @Column(name = "created_by_dashboard_user_id")
    private Long createdByDashboardUserId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
