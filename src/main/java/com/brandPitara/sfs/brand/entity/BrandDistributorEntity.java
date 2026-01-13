package com.brandPitara.sfs.brand.entity;

import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
  name = "brand_distributor",
  uniqueConstraints = @UniqueConstraint(name = "uq_brand_distributor", columnNames = {"brand_id","distributor_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandDistributorEntity extends BaseEntity {

  public enum Status { ACTIVE, INACTIVE }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brand_id", nullable = false)
  private BrandEntity brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "distributor_id", nullable = false)
  private DistributorEntity distributor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private Status status = Status.ACTIVE;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(name = "offer_title", length = 150)
  private String offerTitle;

  @Column(name = "offer_description", columnDefinition = "text")
  private String offerDescription;

  @Column(name = "offer_banner_url", columnDefinition = "text")
  private String offerBannerUrl;

  @Column(name = "valid_till")
  private OffsetDateTime validTill;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean deleted = false;
}
