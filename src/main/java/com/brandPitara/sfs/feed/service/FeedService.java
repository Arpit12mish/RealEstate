package com.brandPitara.sfs.feed.service;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;

public interface FeedService {
  HomeFeedResponse getFeed(FeedScreen screen, Long entityId, Long cityId, Long categoryId, Long clientVersion);
}