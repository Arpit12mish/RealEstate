package com.brandPitara.sfs.company.controller.publicapi;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import com.brandPitara.sfs.company.service.CompanyPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/companies")
@RequiredArgsConstructor
public class CompanyPublicController {

  private final CompanyPublicService companyPublicService;

  @GetMapping("/{companyId}")
  public CompanyResponse get(@PathVariable Long companyId) {
    return companyPublicService.publicGet(companyId);
  }

  @GetMapping
  public Page<CompanyResponse> list(
      @RequestParam(required = false) String companyType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 20),
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );
    return companyPublicService.publicList(companyType, pageable);
  }
}