package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "brand_media",
    indexes = {
        @Index(name = "ix_brand_media_brand_sort", columnList = "brand_id, sort_order"),
        @Index(name = "ix_brand_media_brand_active", columnList = "brand_id, active, deleted")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandMediaEntity extends BaseEntity {

  public enum MediaType { IMAGE, VIDEO }
  public enum Placement { HERO, BANNER, GALLERY, LOGO, PRODUCT }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brand_sku_id")
  private BrandSkuEntity brandSku;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 20)
  private MediaType mediaType;

  @Enumerated(EnumType.STRING)
  @Column(name = "placement", nullable = false, length = 20)
  private Placement placement;

  @Column(nullable = false, columnDefinition = "text")
  private String url;

  @Column(length = 255)
  private String caption;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  // optional: click action for banners
  @Column(name = "action_type", length = 30)
  private String actionType; // e.g., OPEN_URL, OPEN_DISTRIBUTOR, OPEN_CATEGORY

  @Column(name = "action_value", columnDefinition = "text")
  private String actionValue;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean deleted = false;
}
