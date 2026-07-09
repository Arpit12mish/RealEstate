package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "brand_sku",
    indexes = {
        @Index(name = "idx_brand_sku_brand_public", columnList = "brand_id,published,active,deleted,priority"),
        @Index(name = "idx_brand_sku_category",      columnList = "category_id"),
        @Index(name = "idx_brand_sku_product_category", columnList = "product_category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_sku_brand_slug", columnNames = {"brand_id", "slug"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandSkuEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private CategoryEntity category;

  // Distinct from `category` above (the global taxonomy) - this points at this brand's own
  // product category (Lamps/Mirrors/Kitchenware), see BrandProductCategoryEntity.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_category_id")
  private BrandProductCategoryEntity productCategory;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 180)
  private String slug;

  @Column(name = "sku_code", length = 80)
  private String skuCode;

  @Column(name = "short_description", length = 255)
  private String shortDescription;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "price_label", length = 60)
  private String priceLabel;

  // The product detail page on the brand's own external website.
  @Column(name = "external_url", columnDefinition = "text")
  private String externalUrl;

  @Column(nullable = false)
  @Builder.Default
  private Boolean featured = false;

  @Column(nullable = false)
  @Builder.Default
  private Boolean latest = false;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

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
}
