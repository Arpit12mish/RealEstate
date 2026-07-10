package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyPricingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyPricingPlanController {

  private final DashboardCompanyPricingPlanService dashboardCompanyPricingPlanService;

  @GetMapping("/{companyId}/pricing-plans")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyPricingPlanResponse> list(@PathVariable Long companyId) {
    return dashboardCompanyPricingPlanService.list(companyId);
  }

  @PostMapping("/{companyId}/pricing-plans")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyPricingPlanResponse create(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyPricingPlanCreateRequest request
  ) {
    return dashboardCompanyPricingPlanService.create(companyId, request);
  }

  @PatchMapping("/{companyId}/pricing-plans/{planId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyPricingPlanResponse update(
      @PathVariable Long companyId,
      @PathVariable Long planId,
      @Valid @RequestBody CompanyPricingPlanUpdateRequest request
  ) {
    return dashboardCompanyPricingPlanService.update(companyId, planId, request);
  }

  @DeleteMapping("/{companyId}/pricing-plans/{planId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId, @PathVariable Long planId) {
    dashboardCompanyPricingPlanService.delete(companyId, planId);
  }

  @PostMapping("/{companyId}/pricing-plans/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyPricingPlanResponse> reorder(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyPricingPlanReorderRequest request
  ) {
    return dashboardCompanyPricingPlanService.reorder(companyId, request);
  }
}
