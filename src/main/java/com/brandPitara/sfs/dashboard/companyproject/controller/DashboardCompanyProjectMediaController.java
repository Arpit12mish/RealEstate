package com.brandPitara.sfs.dashboard.companyproject.controller;

import com.brandPitara.sfs.company.dto.CompanyProjectMediaResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.*;
import com.brandPitara.sfs.dashboard.companyproject.service.DashboardCompanyProjectMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/company-projects/{companyProjectId}/media")
@RequiredArgsConstructor
public class DashboardCompanyProjectMediaController {
  private final DashboardCompanyProjectMediaService service;

  @GetMapping @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public List<CompanyProjectMediaResponse> list(@PathVariable Long companyProjectId) { return service.list(companyProjectId); }

  @PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectMediaResponse create(@PathVariable Long companyProjectId, @Valid @RequestBody CompanyProjectMediaCreateRequest request) {
    return service.create(companyProjectId, request);
  }

  @PatchMapping("/{mediaId}") @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public CompanyProjectMediaResponse update(@PathVariable Long companyProjectId, @PathVariable Long mediaId, @Valid @RequestBody CompanyProjectMediaUpdateRequest request) {
    return service.update(companyProjectId, mediaId, request);
  }

  @DeleteMapping("/{mediaId}") @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long companyProjectId, @PathVariable Long mediaId) { service.delete(companyProjectId, mediaId); }

  @PostMapping("/reorder") @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public List<CompanyProjectMediaResponse> reorder(@PathVariable Long companyProjectId, @Valid @RequestBody CompanyProjectMediaReorderRequest request) {
    return service.reorder(companyProjectId, request);
  }
}
