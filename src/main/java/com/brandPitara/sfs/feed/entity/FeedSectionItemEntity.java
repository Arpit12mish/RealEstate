package com.brandPitara.sfs.feed.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feed_section_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FeedSectionItemEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "config_id", nullable = false)
  private FeedSectionConfigEntity config;

  @Column(name = "item_type", nullable = false, length = 30)
  private String itemType; // BRAND/BUILDER/BUSINESS/PROJECT

  @Column(name = "ref_id", nullable = false)
  private Long refId;

  @Column(length = 150)
  private String title;

  @Column(length = 255)
  private String subtitle;

  @Column(name = "image_url", columnDefinition = "text")
  private String imageUrl;

  @Column(name = "logo_url", columnDefinition = "text")
  private String logoUrl;

  @Column(name = "group_key", length = 50)
  private String groupKey;

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