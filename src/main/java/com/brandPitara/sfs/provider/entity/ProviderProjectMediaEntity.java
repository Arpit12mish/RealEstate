package com.brandPitara.sfs.provider.entity;

import com.brandPitara.sfs.provider.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_project_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderProjectMediaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private ProviderProjectEntity project;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 10)
  private MediaType mediaType;

  @Column(nullable = false, columnDefinition = "text")
  private String url;

  @Column(columnDefinition = "text")
  private String thumbnailUrl;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private int sortOrder = 0;
}
