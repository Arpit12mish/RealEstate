package com.brandPitara.sfs.feed.dto;

import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FeedResponse {
  private HomeFeedResponse data; // phase-1 wrapper (no refactor)
}