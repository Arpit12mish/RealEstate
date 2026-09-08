package com.brandPitara.sfs.dashboard.companyproject.controller;

import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectDetailResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectListItemResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectUpdateRequest;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/company-projects")
@RequiredArgsConstructor
public class DashboardCompanyProjectController {

  private final DashboardCompanyProjectService dashboardCompanyProjectService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public Page<CompanyProjectListItemResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long companyId,
      @RequestParam(required = false) String companyType,
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50));
    return dashboardCompanyProjectService.list(q, companyId, companyType, cityId, active, pageable);
  }

  @GetMapping("/{companyProjectId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public CompanyProjectDetailResponse getDetail(@PathVariable Long companyProjectId) {
    return dashboardCompanyProjectService.getDetail(companyProjectId);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectDetailResponse create(@Valid @RequestBody CompanyProjectCreateRequest request) {
    return dashboardCompanyProjectService.create(request);
  }

  @PatchMapping("/{companyProjectId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectDetailResponse update(
      @PathVariable Long companyProjectId,
      @Valid @RequestBody CompanyProjectUpdateRequest request
  ) {
    return dashboardCompanyProjectService.update(companyProjectId, request);
  }

  @DeleteMapping("/{companyProjectId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyProjectId) {
    dashboardCompanyProjectService.softDelete(companyProjectId);
  }
}
