package com.brandPitara.sfs.common.contentVersion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_version")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentVersionEntity {

  @Id
  @Column(length = 50)
  private String key; // HOME, BRANDS, BUILDERS

  @Column(nullable = false)
  private long version;
}
