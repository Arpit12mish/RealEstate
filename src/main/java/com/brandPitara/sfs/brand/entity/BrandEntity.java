package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;

import jakarta.persistence.*;
import lombok.*;
import com.brandPitara.sfs.brand.enums.PromoMediaType;

import java.math.BigDecimal;

@Entity
@Table(
    name = "brand",
    indexes = {
        @Index(name = "idx_brand_category_id", columnList = "category_id"),
        @Index(name = "idx_brand_public",      columnList = "published,active,deleted")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_slug", columnNames = "slug")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(nullable = false, length = 180)
  private String slug;

  @Column(name = "hero_image_url", columnDefinition = "text")
  private String heroImageUrl;

  @Column(name = "short_description", length = 255)
  private String shortDescription;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "founded_year")
  private Integer foundedYear;

  @Column(name = "customer_rating", precision = 2, scale = 1)
  private BigDecimal customerRating;

  @Column(name = "customer_rating_count")
  private Integer customerRatingCount;

  // ✅ NEW (promo animation)
  @Enumerated(EnumType.STRING)
  @Column(name = "promo_media_type", length = 20)
  private PromoMediaType promoMediaType; // GIF / LOTTIE

  @Column(name = "promo_media_url", columnDefinition = "text")
  private String promoMediaUrl;

  @Column(name = "promo_enabled", nullable = false)
  @Builder.Default
  private Boolean promoEnabled = false;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean published = false;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private CategoryEntity category;

}
