package com.brandPitara.sfs.dashboard.company.controller;

import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaUpdateRequest;
import com.brandPitara.sfs.dashboard.company.service.DashboardCompanyMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/companies")
@RequiredArgsConstructor
public class DashboardCompanyMediaController {

  private final DashboardCompanyMediaService dashboardCompanyMediaService;

  @GetMapping("/{companyId}/media")
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyMediaResponse> list(@PathVariable Long companyId) {
    return dashboardCompanyMediaService.list(companyId);
  }

  @PostMapping("/{companyId}/media")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyMediaResponse create(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyMediaCreateRequest request
  ) {
    return dashboardCompanyMediaService.create(companyId, request);
  }

  @PatchMapping("/{companyId}/media/{mediaId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyMediaResponse update(
      @PathVariable Long companyId,
      @PathVariable Long mediaId,
      @Valid @RequestBody CompanyMediaUpdateRequest request
  ) {
    return dashboardCompanyMediaService.update(companyId, mediaId, request);
  }

  @DeleteMapping("/{companyId}/media/{mediaId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyId, @PathVariable Long mediaId) {
    dashboardCompanyMediaService.delete(companyId, mediaId);
  }

  @PostMapping("/{companyId}/media/reorder")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyMediaResponse> reorder(
      @PathVariable Long companyId,
      @Valid @RequestBody CompanyMediaReorderRequest request
  ) {
    return dashboardCompanyMediaService.reorder(companyId, request);
  }
}
