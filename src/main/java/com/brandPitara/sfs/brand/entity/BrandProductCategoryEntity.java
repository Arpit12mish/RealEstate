package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A brand's own product category (e.g. "Lamps", "Mirrors", "Kitchenware") shown on that
 * brand's detail page - distinct from the global brand-category taxonomy used for Connected
 * Brands chips/filtering (see BrandCategoryLinkEntity / CategoryEntity). Each row belongs to
 * exactly one brand and links out to that brand's own website via externalUrl.
 */
@Entity
@Table(
    name = "brand_product_category",
    indexes = {
        @Index(name = "idx_brand_product_category_brand_public", columnList = "brand_id,active,public_visible,deleted,sort_order")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandProductCategoryEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 180)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "external_url", columnDefinition = "text")
  private String externalUrl;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "public_visible", nullable = false)
  @Builder.Default
  private Boolean publicVisible = true;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
