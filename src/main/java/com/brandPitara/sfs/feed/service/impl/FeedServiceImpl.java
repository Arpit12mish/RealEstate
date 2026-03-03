package com.brandPitara.sfs.feed.service.impl;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.feed.service.FeedService;
import com.brandPitara.sfs.feed.service.screen.FeedScreenHandler;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedServiceImpl implements FeedService {

  private final Map<FeedScreen, FeedScreenHandler> handlerMap;

  public FeedServiceImpl(List<FeedScreenHandler> handlers) {
    Map<FeedScreen, FeedScreenHandler> m = new EnumMap<>(FeedScreen.class);
    for (FeedScreenHandler h : handlers) {
      m.put(h.supports(), h);
    }
    this.handlerMap = Map.copyOf(m); // immutable (safe)
  }

  @Override
  public HomeFeedResponse getFeed(FeedScreen screen, Long entityId, Long cityId, Long categoryId, Long clientVersion) {
    FeedScreenHandler handler = handlerMap.get(screen);
    if (handler == null) {
      throw new IllegalArgumentException("Screen not implemented yet: " + screen);
    }
    return handler.build(entityId, cityId, categoryId, clientVersion);
  }
}