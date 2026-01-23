package com.brandPitara.sfs.distributor.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "distributor")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 30)
  private String phone;

  @Column(length = 30)
  private String whatsapp;

  @Column(length = 200)
  private String email;

  @Column(name = "logo_url", columnDefinition = "text")
  private String logoUrl;


  @Column(name = "address_line1", length = 255)
  private String addressLine1;

  @Column(name = "address_line2", length = 255)
  private String addressLine2;

  @Column(length = 12)
  private String pincode;

  @Column(name = "city_id")
  private Long cityId;

  private Double latitude;
  private Double longitude;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean deleted = false;
}
