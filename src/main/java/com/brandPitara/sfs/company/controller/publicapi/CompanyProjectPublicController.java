package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.service.CompanyProjectPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CompanyProjectPublicController {

  private final CompanyProjectPublicService companyProjectPublicService;

  // List projects of a company
  @GetMapping("/api/public/companies/{companyId}/projects")
  public Page<CompanyProjectCardDto> listByCompany(
      @PathVariable Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 20),
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );
    return companyProjectPublicService.publicListByCompany(companyId, pageable);
  }

  // Company project detail (separate endpoint)
  @GetMapping("/api/public/company-projects/{companyProjectId}")
  public CompanyProjectResponse get(@PathVariable Long companyProjectId) {
    return companyProjectPublicService.publicGet(companyProjectId);
  }
}