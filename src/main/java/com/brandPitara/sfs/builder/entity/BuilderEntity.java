package com.brandPitara.sfs.builder.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "builder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(columnDefinition = "text")
  private String description;

  // optional contacts
  @Column(length = 20)
  private String phone;

  @Column(length = 20)
  private String whatsapp;

  @Column(length = 150)
  private String email;

  // optional address
  @Column(columnDefinition = "text")
  private String addressLine;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  private Double latitude;
  private Double longitude;

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
