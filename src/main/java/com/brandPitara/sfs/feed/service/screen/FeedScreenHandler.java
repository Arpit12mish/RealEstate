package com.brandPitara.sfs.feed.service.screen;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;

public interface FeedScreenHandler {
  FeedScreen supports();
  HomeFeedResponse build(Long entityId, Long cityId, Long categoryId, Long clientVersion);
}