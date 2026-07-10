package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyMediaEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  @Column(name = "media_url", nullable = false, columnDefinition = "text")
  private String mediaUrl;

  @Column(name = "media_type", nullable = false, length = 20)
  @Builder.Default
  private String mediaType = "IMAGE";

  // HERO | GALLERY | CARD
  @Column(name = "usage_type", nullable = false, length = 20)
  private String usageType;

  @Column(length = 180)
  private String title;

  @Column(name = "alt_text", length = 255)
  private String altText;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "public_visible", nullable = false)
  @Builder.Default
  private Boolean publicVisible = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
