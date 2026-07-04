package com.brandPitara.sfs.home.controller.publicapi;

import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.dto.HomeFeedRequest;
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
      @RequestParam(required = false, name = "v") Long clientVersion,
      @RequestParam(required = false, name = "lat") Double latitude,
      @RequestParam(required = false, name = "lng") Double longitude,
      @RequestParam(required = false) String deviceCity,
      @RequestParam(required = false) Double accuracyMeters
  ) {
    return homeFeedService.getHome(HomeFeedRequest.builder()
        .cityId(cityId)
        .categoryId(categoryId)
        .builderId(builderId)
        .clientVersion(clientVersion)
        .latitude(latitude)
        .longitude(longitude)
        .deviceCity(deviceCity)
        .accuracyMeters(accuracyMeters)
        .build());
  }
}
