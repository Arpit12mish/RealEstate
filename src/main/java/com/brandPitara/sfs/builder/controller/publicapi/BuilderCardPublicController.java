package com.brandPitara.sfs.builder.controller.publicapi;

import com.brandPitara.sfs.builder.dto.BuilderCardResponse;
import com.brandPitara.sfs.builder.service.BuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderCardPublicController {

  private final BuilderService builderService;

  @GetMapping("/card")
  public List<BuilderCardResponse> home(
      @RequestParam(required = false) Long cityId,
      @RequestParam(defaultValue = "12") int limit
  ) {
    return builderService.publicHomeBuilders(cityId, limit);
  }
}
