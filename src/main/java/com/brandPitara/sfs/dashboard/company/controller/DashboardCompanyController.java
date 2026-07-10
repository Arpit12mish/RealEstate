package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyDetailResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyListItemResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyController {

  private final DashboardCompanyService dashboardCompanyService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public Page<CompanyListItemResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String companyType,
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Boolean published,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50));
    return dashboardCompanyService.list(q, companyType, cityId, active, published, pageable);
  }

  @GetMapping("/{companyId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public CompanyDetailResponse getDetail(@PathVariable Long companyId) {
    return dashboardCompanyService.getDetail(companyId);
  }

  // DATA_ENTRY can create company data; ADMIN can too. Companies are created with
  // published = false by default (see DashboardCompanyServiceImpl), safe for the draft flow.
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyDetailResponse create(@Valid @RequestBody CompanyCreateRequest request) {
    return dashboardCompanyService.create(request);
  }

  @PatchMapping("/{companyId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyDetailResponse update(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyUpdateRequest request
  ) {
    return dashboardCompanyService.update(companyId, request);
  }

  @DeleteMapping("/{companyId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId) {
    dashboardCompanyService.softDelete(companyId);
  }
}
