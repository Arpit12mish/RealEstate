package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_project")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(length = 220, unique = true)
  private String slug;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(name = "address_line", columnDefinition = "text")
  private String addressLine;

  @Column(name = "cover_media_url", columnDefinition = "text")
  private String coverMediaUrl;

  @Column(name = "cover_media_type", length = 20)
  private String coverMediaType; // IMAGE | VIDEO | etc

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