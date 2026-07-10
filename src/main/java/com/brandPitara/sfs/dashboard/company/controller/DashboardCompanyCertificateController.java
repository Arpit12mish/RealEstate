package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyCertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyCertificateController {

  private final DashboardCompanyCertificateService dashboardCompanyCertificateService;

  @GetMapping("/{companyId}/certificates")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyCertificateResponse> list(@PathVariable Long companyId) {
    return dashboardCompanyCertificateService.list(companyId);
  }

  @PostMapping("/{companyId}/certificates")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyCertificateResponse create(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyCertificateCreateRequest request
  ) {
    return dashboardCompanyCertificateService.create(companyId, request);
  }

  @PatchMapping("/{companyId}/certificates/{certificateId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyCertificateResponse update(
      @PathVariable Long companyId,
      @PathVariable Long certificateId,
      @Valid @RequestBody CompanyCertificateUpdateRequest request
  ) {
    return dashboardCompanyCertificateService.update(companyId, certificateId, request);
  }

  @DeleteMapping("/{companyId}/certificates/{certificateId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId, @PathVariable Long certificateId) {
    dashboardCompanyCertificateService.delete(companyId, certificateId);
  }

  @PostMapping("/{companyId}/certificates/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyCertificateResponse> reorder(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyCertificateReorderRequest request
  ) {
    return dashboardCompanyCertificateService.reorder(companyId, request);
  }
}
