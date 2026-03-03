package com.brandPitara.sfs.home.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "home_section_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HomeSectionItemEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "home_category_id", nullable = false)
  private CategoryEntity homeCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "section_type", nullable = false, length = 50)
  private HomeSectionType sectionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 30)
  private HomeSectionItemType itemType;

  @Column(name = "ref_id", nullable = false)
  private Long refId;

  @Column(length = 150)
  private String title;

  @Column(length = 255)
  private String subtitle;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "config_id", nullable = false)
  private HomeSectionConfigEntity config;

  @Column(name = "group_key", length = 50)
  private String groupKey;


}
