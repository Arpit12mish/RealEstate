package com.brandPitara.sfs.home.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAnalyticsCardDto {
  private Long id;
  private String title;
  private String imageUrl;
  private String caption;
}
