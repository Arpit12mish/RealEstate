package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandCollaborationResponse;
import com.brandPitara.sfs.brand.dto.BrandCollaborationUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandCollaborationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandCollaborationController {

  private final BrandCollaborationService brandCollaborationService;

  @PostMapping("/{brandId}/collaborations")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandCollaborationResponse create(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandCollaborationUpsertRequest request
  ) {
    return brandCollaborationService.create(brandId, request);
  }

  @PutMapping("/{brandId}/collaborations/{collaborationId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandCollaborationResponse update(
      @PathVariable Long brandId,
      @PathVariable Long collaborationId,
      @Valid @RequestBody BrandCollaborationUpsertRequest request
  ) {
    return brandCollaborationService.update(brandId, collaborationId, request);
  }

  @GetMapping("/{brandId}/collaborations")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<BrandCollaborationResponse> list(
      @PathVariable Long brandId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("sortOrder").ascending().and(Sort.by("id").ascending()));
    return brandCollaborationService.adminList(brandId, pageable);
  }

  @DeleteMapping("/{brandId}/collaborations/{collaborationId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long collaborationId) {
    brandCollaborationService.softDelete(brandId, collaborationId);
  }
}
