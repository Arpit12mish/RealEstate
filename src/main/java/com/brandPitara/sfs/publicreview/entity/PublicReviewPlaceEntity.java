package com.brandPitara.sfs.publicreview.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewDisplayMode;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewFetchStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewPlaceCategory;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "public_review_place",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_public_review_place_target_google",
            columnNames = {"target_type", "target_id", "google_place_id"}
        )
    },
    indexes = {
        @Index(name = "idx_public_review_place_target", columnList = "target_type,target_id"),
        @Index(name = "idx_public_review_place_google_place_id", columnList = "google_place_id"),
        @Index(name = "idx_public_review_place_active_lookup", columnList = "target_type,target_id,active,deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReviewPlaceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PublicReviewTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    @Builder.Default
    private PublicReviewSourceType sourceType = PublicReviewSourceType.GOOGLE_PLACES;

    @Column(name = "google_place_id", nullable = false, length = 255)
    private String googlePlaceId;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "formatted_address", columnDefinition = "text")
    private String formattedAddress;

    @Column(name = "google_maps_uri", columnDefinition = "text")
    private String googleMapsUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_category", nullable = false, length = 60)
    @Builder.Default
    private PublicReviewPlaceCategory placeCategory = PublicReviewPlaceCategory.OTHER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "one_time_fetched", nullable = false)
    @Builder.Default
    private Boolean oneTimeFetched = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_status", nullable = false, length = 30)
    @Builder.Default
    private GoogleReviewFetchStatus fetchStatus = GoogleReviewFetchStatus.NOT_FETCHED;

    @Column(name = "content_expires_at")
    private OffsetDateTime contentExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 30)
    @Builder.Default
    private GoogleReviewDisplayMode displayMode = GoogleReviewDisplayMode.HIDDEN;

    @Column(name = "display_google_reviews", nullable = false)
    @Builder.Default
    private Boolean displayGoogleReviews = false;
}
