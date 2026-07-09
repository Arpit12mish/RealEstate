package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandProductCategoryResponse;
import com.brandPitara.sfs.brand.dto.BrandProductCategoryUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandProductCategoryService;
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
public class AdminBrandProductCategoryController {

  private final BrandProductCategoryService brandProductCategoryService;

  @PostMapping("/{brandId}/product-categories")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandProductCategoryResponse create(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandProductCategoryUpsertRequest request
  ) {
    return brandProductCategoryService.create(brandId, request);
  }

  // Existing Brand sub-resource controllers (certificates, SKUs) use PUT for
  // partial-field-update semantics, not PATCH - matched here for consistency.
  @PutMapping("/{brandId}/product-categories/{categoryId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandProductCategoryResponse update(
      @PathVariable Long brandId,
      @PathVariable Long categoryId,
      @Valid @RequestBody BrandProductCategoryUpsertRequest request
  ) {
    return brandProductCategoryService.update(brandId, categoryId, request);
  }

  @GetMapping("/{brandId}/product-categories")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public Page<BrandProductCategoryResponse> list(
      @PathVariable Long brandId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("sortOrder").ascending().and(Sort.by("id").ascending()));
    return brandProductCategoryService.adminList(brandId, pageable);
  }

  @GetMapping("/{brandId}/product-categories/{categoryId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public BrandProductCategoryResponse getById(@PathVariable Long brandId, @PathVariable Long categoryId) {
    return brandProductCategoryService.adminGetById(brandId, categoryId);
  }

  @DeleteMapping("/{brandId}/product-categories/{categoryId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long categoryId) {
    brandProductCategoryService.softDelete(brandId, categoryId);
  }
}
