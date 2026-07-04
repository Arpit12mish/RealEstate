package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "promo_banner",
    indexes = {
        @Index(name = "idx_promo_banner_category_id",  columnList = "category_id"),
        @Index(name = "idx_promo_banner_active_slot",  columnList = "is_active,slot_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoBannerEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "media_type", nullable = false, length = 30)
    @Builder.Default
    private String mediaType = "IMAGE";

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "display_duration_ms")
    private Integer displayDurationMs;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ✅ NEW: used for placements (HERO/MID/BOTTOM/SEARCH_TOP etc.)
    @Column(name = "slot_key", nullable = false, length = 20)
    @Builder.Default
    private String slotKey = "HERO";

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;
}
