package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;

import jakarta.persistence.*;
import lombok.*;
import com.brandPitara.sfs.brand.enums.PromoMediaType;

@Entity
@Table(name = "brand")
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

  @Column(columnDefinition = "text")
  private String description;

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
