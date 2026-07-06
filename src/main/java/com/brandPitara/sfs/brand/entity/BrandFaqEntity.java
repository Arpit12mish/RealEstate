package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brand_faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandFaqEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @Column(nullable = false, length = 300)
  private String question;

  @Column(nullable = false, columnDefinition = "text")
  private String answer;

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
