package com.brandPitara.sfs.home.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "home_section_config",
    indexes = {
        @Index(name = "idx_home_section_category_id", columnList = "home_category_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HomeSectionConfigEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "home_category_id", nullable = false)
  private CategoryEntity homeCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "section_type", nullable = false, length = 50)
  private HomeSectionType sectionType;

  @Column(length = 150)
  private String title;

  @Column(length = 255)
  private String subtitle;

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "max_items", nullable = false)
  @Builder.Default
  private Integer maxItems = 10;

  @Column(length = 200)
  private String param1;

  @Column(length = 200)
  private String param2;

  @Column(length = 200)
  private String param3;
}
