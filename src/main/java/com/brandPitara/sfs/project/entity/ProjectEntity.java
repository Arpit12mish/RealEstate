package com.brandPitara.sfs.project.entity;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "builder_id", nullable = false)
  private BuilderEntity builder;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(length = 220, unique = true)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(columnDefinition = "text")
  private String addressLine;

  private Double latitude;
  private Double longitude;

  private Long priceMin;
  private Long priceMax;

  private LocalDate possessionDate;

  @Column(length = 60)
  private String reraNumber;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private ProjectStatus status;

  // For EnumSet, easiest + efficient is ElementCollection
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "project_property_types", joinColumns = @JoinColumn(name = "project_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", length = 30)
  @Builder.Default
  private Set<PropertyType> propertyTypes = new HashSet<>();

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
