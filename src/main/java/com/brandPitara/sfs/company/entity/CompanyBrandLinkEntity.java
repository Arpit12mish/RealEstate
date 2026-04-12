package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "company_brand_link",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_brand_link_company_brand", columnNames = {"company_id", "brand_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyBrandLinkEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

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