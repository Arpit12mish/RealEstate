package com.brandPitara.sfs.home.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeaturedCarouselCardDto {
  private String variant;     // TALL | SMALL_TOP | SMALL_BOTTOM
  private Integer position;   // 1..3

  private String title;
  private String subtitle;

  private String imageUrl;
  private String logoUrl;

  private String entityType;  // BUILDER | BRAND | DESIGNER | URL
  private Long entityId;
  private String targetUrl;
}
