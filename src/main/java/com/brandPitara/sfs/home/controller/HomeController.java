// com.brandPitara.sfs.home.controller.HomeController.java
package com.brandPitara.sfs.home.controller;

import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.service.HomeFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

  private final HomeFeedService homeFeedService;

  @GetMapping
  public HomeFeedResponse getHome(@RequestParam(required = false) Long cityId) {
    return homeFeedService.getHome(cityId);
  }
}
