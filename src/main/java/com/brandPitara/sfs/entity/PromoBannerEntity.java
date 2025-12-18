package com.brandPitara.sfs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "promo_banner")
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

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "start_at")
    private java.time.OffsetDateTime startAt;

    @Column(name = "end_at")
    private java.time.OffsetDateTime endAt;
}
