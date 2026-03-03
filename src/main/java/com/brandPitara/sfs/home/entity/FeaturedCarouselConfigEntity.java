package com.brandPitara.sfs.home.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "featured_carousel_config")
public class FeaturedCarouselConfigEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "city_id")
  private Long cityId;

  @Column(name = "category_id", nullable = false)
  private Long categoryId;

  @Column(name = "variant", nullable = false, length = 20)
  private String variant; // TALL | SMALL_TOP | SMALL_BOTTOM

  @Column(name = "position", nullable = false)
  private Integer position; // 1..3

  @Column(name = "title", nullable = false, length = 180)
  private String title;

  @Column(name = "subtitle", length = 220)
  private String subtitle;

  @Column(name = "image_url", nullable = false, columnDefinition = "text")
  private String imageUrl;

  @Column(name = "logo_url", columnDefinition = "text")
  private String logoUrl;

  @Column(name = "entity_type", length = 20)
  private String entityType; // BUILDER | BRAND | DESIGNER | URL

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "target_url", columnDefinition = "text")
  private String targetUrl;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "priority", nullable = false)
  private Integer priority = 0;

  @Column(name = "start_at")
  private Instant startAt;

  @Column(name = "end_at")
  private Instant endAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    var now = Instant.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (active == null) active = true;
    if (priority == null) priority = 0;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
