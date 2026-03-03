package com.brandPitara.sfs.feed.dto;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FeedRequest {
  private FeedScreen screen;

  private Long cityId;
  private Long categoryId;
  private Long entityId;     // builderId/brandId/projectId depending on screen

  private Long clientVersion; // optional
}