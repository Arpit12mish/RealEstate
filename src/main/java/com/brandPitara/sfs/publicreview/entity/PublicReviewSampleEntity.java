package com.brandPitara.sfs.publicreview.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewDisplayStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSentiment;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "public_review_sample",
    indexes = {
        @Index(name = "idx_public_review_sample_target", columnList = "target_type,target_id"),
        @Index(name = "idx_public_review_sample_place", columnList = "review_place_id"),
        @Index(name = "idx_public_review_sample_public_lookup", columnList = "review_place_id,display_status,rating"),
        @Index(name = "idx_public_review_sample_rating", columnList = "rating"),
        @Index(name = "idx_public_review_sample_sentiment", columnList = "sentiment")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewSampleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Column(name = "reviewer_name", length = 255)
    private String reviewerName;

    @Column(name = "reviewer_profile_url", columnDefinition = "text")
    private String reviewerProfileUrl;

    @Column(name = "reviewer_photo_url", columnDefinition = "text")
    private String reviewerPhotoUrl;

    private Integer rating;

    @Column(name = "review_text", columnDefinition = "text")
    private String reviewText;

    @Column(name = "original_review_text", columnDefinition = "text")
    private String originalReviewText;

    @Column(name = "language_code", length = 20)
    private String languageCode;

    @Column(name = "relative_publish_time", length = 100)
    private String relativePublishTime;

    @Column(name = "publish_time")
    private OffsetDateTime publishTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PublicReviewSentiment sentiment;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 30)
    @Builder.Default
    private PublicReviewDisplayStatus displayStatus = PublicReviewDisplayStatus.INTERNAL_ONLY;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private OffsetDateTime fetchedAt = OffsetDateTime.now();
}
