package com.brandPitara.sfs.dashboard.companyproject.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedReorderRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedUpdateRequest;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectBrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/company-projects")
@RequiredArgsConstructor
public class DashboardCompanyProjectBrandController {

  private final DashboardCompanyProjectBrandService dashboardCompanyProjectBrandService;
  private final DashboardActionAuditService dashboardActionAuditService;

  @GetMapping("/{companyProjectId}/brands-used")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyProjectBrandUsedResponse> list(@PathVariable Long companyProjectId) {
    return dashboardCompanyProjectBrandService.list(companyProjectId);
  }

  @PostMapping("/{companyProjectId}/brands-used")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectBrandUsedResponse create(
      @PathVariable Long companyProjectId,
      @Valid @RequestBody CompanyProjectBrandUsedCreateRequest request
  ) {
    CompanyProjectBrandUsedResponse response = dashboardCompanyProjectBrandService.create(companyProjectId, request);
    dashboardActionAuditService.record(
        DashboardAuditAction.COMPANY_PROJECT_BRAND_CREATED, ReviewEntityType.COMPANY_PROJECT_BRAND, response.getId(), null
    );
    return response;
  }

  @PatchMapping("/{companyProjectId}/brands-used/{collaborationId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectBrandUsedResponse update(
      @PathVariable Long companyProjectId,
      @PathVariable Long collaborationId,
      @Valid @RequestBody CompanyProjectBrandUsedUpdateRequest request
  ) {
    CompanyProjectBrandUsedResponse response =
        dashboardCompanyProjectBrandService.update(companyProjectId, collaborationId, request);
    dashboardActionAuditService.record(
        DashboardAuditAction.COMPANY_PROJECT_BRAND_UPDATED, ReviewEntityType.COMPANY_PROJECT_BRAND, collaborationId, null
    );
    return response;
  }

  @DeleteMapping("/{companyProjectId}/brands-used/{collaborationId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyProjectId, @PathVariable Long collaborationId) {
    dashboardCompanyProjectBrandService.delete(companyProjectId, collaborationId);
    dashboardActionAuditService.record(
        DashboardAuditAction.COMPANY_PROJECT_BRAND_DELETED, ReviewEntityType.COMPANY_PROJECT_BRAND, collaborationId, null
    );
  }

  @PostMapping("/{companyProjectId}/brands-used/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyProjectBrandUsedResponse> reorder(
      @PathVariable Long companyProjectId,
      @Valid @RequestBody CompanyProjectBrandUsedReorderRequest request
  ) {
    return dashboardCompanyProjectBrandService.reorder(companyProjectId, request);
  }
}
