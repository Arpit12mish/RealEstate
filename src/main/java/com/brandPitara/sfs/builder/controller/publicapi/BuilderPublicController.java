package com.brandPitara.sfs.builder.controller.publicapi;

import com.brandPitara.sfs.builder.dto.BuilderPublicResponse;
import com.brandPitara.sfs.builder.service.BuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderPublicController {

  private final BuilderService builderService;

  @GetMapping
  public Page<BuilderPublicResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("priority").ascending().and(Sort.by("id").descending()));
    return builderService.listPublished(pageable);
  }

  @GetMapping("/{id}")
  public BuilderPublicResponse get(@PathVariable Long id) {
    return builderService.publicGetById(id);
  }
}
