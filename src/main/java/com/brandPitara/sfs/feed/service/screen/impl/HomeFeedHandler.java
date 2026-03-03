package com.brandPitara.sfs.feed.service.screen.impl;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.feed.service.screen.FeedScreenHandler;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.service.HomeFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HomeFeedHandler implements FeedScreenHandler {

  private final HomeFeedService homeFeedService;

  @Override
  public FeedScreen supports() {
    return FeedScreen.HOME;
  }

  @Override
  public HomeFeedResponse build(Long entityId, Long cityId, Long categoryId, Long clientVersion) {
    return homeFeedService.getHome(cityId, categoryId, null, clientVersion);
  }
}