package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 180, unique = true)
  private String slug;

  @Column(name = "company_type", nullable = false, length = 40)
  private String companyType;

  @Column(name = "logo_url", columnDefinition = "text")
  private String logoUrl;

  @Column(name = "cover_image_url", columnDefinition = "text")
  private String coverImageUrl;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "specialization_text", length = 255)
  private String specializationText;

  @Column(name = "info_line_1", columnDefinition = "text")
  private String infoLine1;

  @Column(name = "info_line_2", columnDefinition = "text")
  private String infoLine2;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(name = "address_line", columnDefinition = "text")
  private String addressLine;

  // Comma-separated free text, parsed the same way CompanyProjectEntity.tags is
  // (see CompanyProjectTagMapper) - no separate normalized table needed for a
  // simple editorial list like this.
  @Column(name = "services_offered", columnDefinition = "text")
  private String servicesOffered;

  @Column(length = 20)
  private String phone;

  @Column(length = 20)
  private String whatsapp;

  @Column(length = 150)
  private String email;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean published = true;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}