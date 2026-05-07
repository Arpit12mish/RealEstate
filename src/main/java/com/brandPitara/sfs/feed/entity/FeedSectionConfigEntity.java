package com.brandPitara.sfs.feed.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "feed_section_config",
    indexes = {
        @Index(name = "idx_feed_section_screen",      columnList = "screen"),
        @Index(name = "idx_feed_section_category_id", columnList = "category_id"),
        @Index(name = "idx_feed_section_city_id",     columnList = "city_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FeedSectionConfigEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 30)
  private String screen; // store as String for flexibility (maps from enum)

  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "city_id")
  private Long cityId;

  @Column(name = "section_type", nullable = false, length = 80)
  private String sectionType; // String = future-proof

  @Column(length = 150)
  private String title;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "max_items", nullable = false)
  @Builder.Default
  private Integer maxItems = 10;

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;

  @Column(length = 500, name = "param1", columnDefinition = "jsonb") private String param1;
  @Column(length = 200) private String param2;
  @Column(length = 200) private String param3;
}