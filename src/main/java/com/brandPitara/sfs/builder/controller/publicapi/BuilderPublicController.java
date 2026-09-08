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

  // Canonical slug-based detail lookup. The path constraint requires at least one
  // alphabetic character, so a purely-numeric path can never match this pattern -
  // mirrors BrandPublicController's own slug-vs-numeric-id disambiguation approach,
  // though here it's not strictly required for route matching (this endpoint lives
  // under a distinct /slug/ prefix, so it can never collide with GET /{id} above);
  // it still doubles as slug-format validation, rejecting obviously malformed input
  // with a clean 404 before it ever reaches the service/repository layer.
  @GetMapping("/slug/{builderSlug:[a-z0-9-]*[a-z][a-z0-9-]*}")
  public BuilderPublicResponse getBySlug(@PathVariable String builderSlug) {
    return builderService.publicGetBySlug(builderSlug);
  }
}
