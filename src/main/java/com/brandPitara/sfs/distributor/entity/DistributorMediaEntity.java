package com.brandPitara.sfs.distributor.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "distributor_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DistributorMediaEntity extends BaseEntity {

  public enum MediaType { IMAGE, VIDEO }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "distributor_id", nullable = false)
  private DistributorEntity distributor;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 20)
  private MediaType mediaType;

  @Column(nullable = false, columnDefinition = "text")
  private String url;

  @Column(length = 255)
  private String caption;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean deleted = false;
}
