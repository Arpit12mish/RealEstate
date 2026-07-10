package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyStatCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyStatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyStatController {

  private final DashboardCompanyStatService dashboardCompanyStatService;

  @GetMapping("/{companyId}/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyStatResponse> list(@PathVariable Long companyId) {
    return dashboardCompanyStatService.list(companyId);
  }

  @PostMapping("/{companyId}/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyStatResponse create(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyStatCreateRequest request
  ) {
    return dashboardCompanyStatService.create(companyId, request);
  }

  @PatchMapping("/{companyId}/stats/{statId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyStatResponse update(
      @PathVariable Long companyId,
      @PathVariable Long statId,
      @Valid @RequestBody CompanyStatUpdateRequest request
  ) {
    return dashboardCompanyStatService.update(companyId, statId, request);
  }

  @DeleteMapping("/{companyId}/stats/{statId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId, @PathVariable Long statId) {
    dashboardCompanyStatService.delete(companyId, statId);
  }

  @PostMapping("/{companyId}/stats/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyStatResponse> reorder(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyStatReorderRequest request
  ) {
    return dashboardCompanyStatService.reorder(companyId, request);
  }
}
