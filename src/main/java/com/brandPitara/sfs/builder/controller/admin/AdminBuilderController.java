package com.brandPitara.sfs.builder.controller.admin;

import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.service.BuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/builders")
@RequiredArgsConstructor
public class AdminBuilderController {

  private final BuilderService builderService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public BuilderResponse create(@Valid @RequestBody BuilderUpsertRequest request) {
    return builderService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BuilderResponse update(@PathVariable Long id, @Valid @RequestBody BuilderUpsertRequest request) {
    return builderService.update(id, request);
  }

  @PatchMapping("/{id}/published")
  @PreAuthorize("hasRole('ADMIN')")
  public BuilderResponse setPublished(@PathVariable Long id, @RequestParam boolean value) {
    return builderService.setPublished(id, value);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    builderService.softDelete(id);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BuilderResponse get(@PathVariable Long id) {
    return builderService.getById(id);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Page<BuilderResponse> list(
      @RequestParam(required = false) Boolean published,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("priority").ascending().and(Sort.by("id").descending()));
    return builderService.adminList(published, active, pageable);
  }
}
