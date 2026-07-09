package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandDistributorResponse;
import com.brandPitara.sfs.brand.dto.BrandDistributorUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandDistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandDistributorController {

  private final BrandDistributorService brandDistributorService;

  @PostMapping("/{brandId}/distributors")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandDistributorResponse upsert(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandDistributorUpsertRequest request
  ) {
    return brandDistributorService.upsert(brandId, request);
  }

  @GetMapping("/{brandId}/distributors")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public Page<BrandDistributorResponse> list(
      @PathVariable Long brandId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("priority").ascending());
    return brandDistributorService.adminList(brandId, pageable);
  }

  @DeleteMapping("/{brandId}/distributors/{distributorId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(
      @PathVariable Long brandId,
      @PathVariable Long distributorId
  ) {
    brandDistributorService.softDelete(brandId, distributorId);
  }
}
