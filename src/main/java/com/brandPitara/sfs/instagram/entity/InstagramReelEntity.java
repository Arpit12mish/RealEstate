package com.brandPitara.sfs.instagram.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "instagram_reel",
    indexes = {
        @Index(name = "idx_instagram_reel_active_published", columnList = "active, deleted, published_at"),
        @Index(name = "idx_instagram_reel_active_trending", columnList = "active, deleted, trending_score"),
        @Index(name = "idx_instagram_reel_active_view_count", columnList = "active, deleted, view_count"),
        @Index(name = "idx_instagram_reel_category_override", columnList = "category_override"),
        @Index(name = "idx_instagram_reel_display_order", columnList = "display_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramReelEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instagram_media_id", length = 100)
    private String instagramMediaId;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(length = 180)
    private String title;

    @Column(name = "instagram_url", nullable = false, length = 500)
    private String instagramUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "preview_video_url", length = 1000)
    private String previewVideoUrl;

    @Column(name = "media_type", length = 50)
    private String mediaType;

    @Column(name = "media_product_type", length = 50)
    private String mediaProductType;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Long commentCount = 0L;

    @Column(name = "share_count", nullable = false)
    @Builder.Default
    private Long shareCount = 0L;

    @Column(name = "save_count", nullable = false)
    @Builder.Default
    private Long saveCount = 0L;

    @Column(name = "trending_score", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal trendingScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_override", length = 50)
    private InstagramReelCategory categoryOverride;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "synced_from_meta", nullable = false)
    @Builder.Default
    private Boolean syncedFromMeta = true;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
