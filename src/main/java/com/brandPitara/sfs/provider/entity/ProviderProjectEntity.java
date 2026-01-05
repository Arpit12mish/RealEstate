package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.provider.enums.ProjectVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provider_project")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderProjectEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "provider_id", nullable = false)
  private ProviderProfileEntity provider;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  private String locality;

  private Integer budgetMin;
  private Integer budgetMax;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ProjectVisibility visibility = ProjectVisibility.PUBLIC;

  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC")
  @Builder.Default
  private List<ProviderProjectMediaEntity> media = new ArrayList<>();
}
