package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandCertificateResponse;
import com.brandPitara.sfs.brand.dto.BrandCertificateUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandCertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandCertificateController {

  private final BrandCertificateService brandCertificateService;

  @PostMapping("/{brandId}/certificates")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandCertificateResponse create(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandCertificateUpsertRequest request
  ) {
    return brandCertificateService.create(brandId, request);
  }

  @PutMapping("/{brandId}/certificates/{certificateId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandCertificateResponse update(
      @PathVariable Long brandId,
      @PathVariable Long certificateId,
      @Valid @RequestBody BrandCertificateUpsertRequest request
  ) {
    return brandCertificateService.update(brandId, certificateId, request);
  }

  @GetMapping("/{brandId}/certificates")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<BrandCertificateResponse> list(@PathVariable Long brandId) {
    return brandCertificateService.adminList(brandId);
  }

  @DeleteMapping("/{brandId}/certificates/{certificateId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long certificateId) {
    brandCertificateService.softDelete(brandId, certificateId);
  }
}
