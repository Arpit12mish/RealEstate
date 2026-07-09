package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandCategoryLinkResponse;
import com.brandPitara.sfs.brand.dto.BrandCategoryLinkUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandCategoryLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandCategoryLinkController {

  private final BrandCategoryLinkService brandCategoryLinkService;

  @PostMapping("/{brandId}/categories")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandCategoryLinkResponse upsert(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandCategoryLinkUpsertRequest request
  ) {
    return brandCategoryLinkService.upsert(brandId, request);
  }

  @GetMapping("/{brandId}/categories")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<BrandCategoryLinkResponse> list(@PathVariable Long brandId) {
    return brandCategoryLinkService.adminList(brandId);
  }

  @DeleteMapping("/{brandId}/categories/{categoryId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long categoryId) {
    brandCategoryLinkService.softDelete(brandId, categoryId);
  }
}
