package com.brandPitara.sfs.feed.controller.publicapi;

import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.feed.service.FeedService;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/feed")
@RequiredArgsConstructor
public class FeedPublicController {

  private final FeedService feedService;

  @GetMapping
  public HomeFeedResponse getFeed(
      @RequestParam FeedScreen screen,
      @RequestParam(required = false) Long entityId,
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false, name = "v") Long clientVersion
  ) {
    return feedService.getFeed(screen, entityId, cityId, categoryId, clientVersion);
  }
}