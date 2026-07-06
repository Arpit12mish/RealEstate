package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "brand_category_link",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_category_link_brand_category", columnNames = {"brand_id", "category_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandCategoryLinkEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
