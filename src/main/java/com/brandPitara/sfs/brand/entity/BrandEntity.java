package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
