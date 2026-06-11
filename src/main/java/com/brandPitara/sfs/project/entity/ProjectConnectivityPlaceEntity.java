package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "project_connectivity_place")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "connectivity_id")
  private ProjectConnectivityEntity connectivity;

  @Column(name = "place_name", nullable = false, length = 180)
  private String placeName;

  @Enumerated(EnumType.STRING)
  @Column(name = "place_type", nullable = false, length = 60)
  private ProjectConnectivityType placeType;

  @Column(name = "distance_label", length = 80)
  private String distanceLabel;

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

  // Map intelligence fields
  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "distance_meters")
  private Integer distanceMeters;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "duration_label", length = 80)
  private String durationLabel;

  @Column(name = "external_place_id", length = 180)
  private String externalPlaceId;

  @Column(name = "provider", length = 40)
  private String provider;

  @Column(name = "rating", precision = 3, scale = 2)
  private BigDecimal rating;

  @Column(name = "user_rating_count")
  private Integer userRatingCount;

  @Column(name = "address", columnDefinition = "text")
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", length = 40)
  private ProjectConnectivityCategory category;

  @Column(name = "verified", nullable = false)
  @Builder.Default
  private Boolean verified = false;

  @Column(name = "featured", nullable = false)
  @Builder.Default
  private Boolean featured = false;
}
