package com.brandPitara.sfs.brand.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandResponse {
  private Long id;
  private String name;
  private String logoUrl;
  private String description;
  private boolean active;
  private boolean published;
  private int priority;
}
