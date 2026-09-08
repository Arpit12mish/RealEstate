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

  // Canonical slug-based detail lookup - the future website Company Detail route's actual
  // backend contract (kept unbuilt on the website side this phase). The path constraint
  // requires at least one alphabetic character, so a purely-numeric path can never match
  // this pattern - mirrors BuilderPublicController's own slug endpoint (added the same way,
  // same phase family). Not strictly required for route matching here either (this lives
  // under a distinct /slug/ prefix, so it can never collide with GET /{companyId} above);
  // it still doubles as slug-format validation, rejecting obviously malformed input with a
  // clean 404 (no route matches) before it ever reaches the service/repository layer.
  @GetMapping("/slug/{companySlug:[a-z0-9-]*[a-z][a-z0-9-]*}")
  public CompanyResponse getBySlug(@PathVariable String companySlug) {
    return companyPublicService.publicGetBySlug(companySlug);
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