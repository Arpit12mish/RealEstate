package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyConnectedBrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyConnectedBrandController {

  private final DashboardCompanyConnectedBrandService dashboardCompanyConnectedBrandService;

  @GetMapping("/{companyId}/connected-brands")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyConnectedBrandResponse> list(@PathVariable Long companyId) {
    return dashboardCompanyConnectedBrandService.list(companyId);
  }

  @PostMapping("/{companyId}/connected-brands")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyConnectedBrandResponse create(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyConnectedBrandCreateRequest request
  ) {
    return dashboardCompanyConnectedBrandService.create(companyId, request);
  }

  @PatchMapping("/{companyId}/connected-brands/{collaborationId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyConnectedBrandResponse update(
      @PathVariable Long companyId,
      @PathVariable Long collaborationId,
      @Valid @RequestBody CompanyConnectedBrandUpdateRequest request
  ) {
    return dashboardCompanyConnectedBrandService.update(companyId, collaborationId, request);
  }

  @DeleteMapping("/{companyId}/connected-brands/{collaborationId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId, @PathVariable Long collaborationId) {
    dashboardCompanyConnectedBrandService.delete(companyId, collaborationId);
  }
}
