package com.brandPitara.sfs.home.controller.publicapi;

import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.service.HomeFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class HomePublicController {

  private final HomeFeedService homeFeedService;

  @GetMapping
  public HomeFeedResponse getHome(
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long builderId,
      @RequestParam(required = false, name = "v") Long clientVersion
  ) {
    return homeFeedService.getHome(cityId, categoryId, builderId, clientVersion);
  }
}
