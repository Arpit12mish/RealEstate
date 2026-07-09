package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandSkuResponse;
import com.brandPitara.sfs.brand.dto.BrandSkuUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandSkuService;
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
public class AdminBrandSkuController {

  private final BrandSkuService brandSkuService;

  @PostMapping("/{brandId}/skus")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandSkuResponse create(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandSkuUpsertRequest request
  ) {
    return brandSkuService.create(brandId, request);
  }

  @PutMapping("/{brandId}/skus/{skuId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandSkuResponse update(
      @PathVariable Long brandId,
      @PathVariable Long skuId,
      @Valid @RequestBody BrandSkuUpsertRequest request
  ) {
    return brandSkuService.update(brandId, skuId, request);
  }

  @GetMapping("/{brandId}/skus")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public Page<BrandSkuResponse> list(
      @PathVariable Long brandId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("sortOrder").ascending().and(Sort.by("id").ascending()));
    return brandSkuService.adminList(brandId, pageable);
  }

  @GetMapping("/{brandId}/skus/{skuId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public BrandSkuResponse getById(@PathVariable Long brandId, @PathVariable Long skuId) {
    return brandSkuService.adminGetById(brandId, skuId);
  }

  @DeleteMapping("/{brandId}/skus/{skuId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long skuId) {
    brandSkuService.softDelete(brandId, skuId);
  }
}
