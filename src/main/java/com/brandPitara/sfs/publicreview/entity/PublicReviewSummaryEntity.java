package com.brandPitara.sfs.publicreview.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "public_review_summary",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_public_review_summary_place", columnNames = "review_place_id")
    },
    indexes = {
        @Index(name = "idx_public_review_summary_target", columnList = "target_type,target_id"),
        @Index(name = "idx_public_review_summary_place", columnList = "review_place_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewSummaryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_place_id", nullable = false)
    private PublicReviewPlaceEntity reviewPlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PublicReviewTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    @Builder.Default
    private PublicReviewSourceType sourceType = PublicReviewSourceType.GOOGLE_PLACES;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "user_rating_count")
    private Integer userRatingCount;

    @Column(name = "positive_sample_count", nullable = false)
    @Builder.Default
    private Integer positiveSampleCount = 0;

    @Column(name = "negative_sample_count", nullable = false)
    @Builder.Default
    private Integer negativeSampleCount = 0;

    @Column(name = "neutral_sample_count", nullable = false)
    @Builder.Default
    private Integer neutralSampleCount = 0;

    @Column(name = "mixed_sample_count", nullable = false)
    @Builder.Default
    private Integer mixedSampleCount = 0;

    @Column(name = "source_label", nullable = false, length = 100)
    @Builder.Default
    private String sourceLabel = "Google Maps";

    @Column(columnDefinition = "text")
    private String disclaimer;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;
}
