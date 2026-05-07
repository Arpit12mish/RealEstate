package com.brandPitara.sfs.home.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
// import com.brandPitara.sfs.home.enums.FeedScreen;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "promo_banner_slot_config",
    indexes = {
        @Index(name = "idx_promo_slot_category_id", columnList = "home_category_id"),
        @Index(name = "idx_promo_slot_city_id",     columnList = "city_id"),
        @Index(name = "idx_promo_slot_screen",      columnList = "screen")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PromoBannerSlotConfigEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FeedScreen screen;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "home_category_id", nullable = false)
  private CategoryEntity homeCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(name = "slot_key", nullable = false, length = 20)
  @Builder.Default
  private String slotKey = "HERO";

  
  @Column(name = "insert_after_section_type", length = 50)
  private String insertAfterSectionType; // ex: "TOP_PROJECTS", "ABOUT", "REVIEWS"

  @Column(name = "position_index", nullable = false)
  @Builder.Default
  private Integer positionIndex = 0;

  @Column(name = "max_items", nullable = false)
  @Builder.Default
  private Integer maxItems = 10;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "start_at")
  private OffsetDateTime startAt;

  @Column(name = "end_at")
  private OffsetDateTime endAt;
}