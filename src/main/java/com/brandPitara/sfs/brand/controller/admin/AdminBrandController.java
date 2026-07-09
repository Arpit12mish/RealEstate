package com.brandPitara.sfs.brand.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

  private final BrandService brandService;

  // DATA_ENTRY can create brand data; ADMIN can too. Brands are created with
  // published = false by default (see BrandServiceImpl), safe for the dashboard draft flow.
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandResponse create(@Valid @RequestBody BrandUpsertRequest request) {
    return brandService.create(request);
  }

  // DATA_ENTRY can update brand data; ADMIN can too.
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandUpsertRequest request) {
    return brandService.update(id, request);
  }

  // ADMIN-only direct publish, matching DashboardProjectController#setPublished - publishing
  // is a review/approval action, not a data-entry action.
  @PatchMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandResponse publish(@PathVariable Long id, @RequestParam boolean published) {
    return brandService.setPublished(id, published);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    brandService.softDelete(id);
  }

  // All dashboard roles can view brand details.
  @GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
public BrandResponse getById(@PathVariable Long id) {
  return brandService.adminGetById(id);
}

// All dashboard roles can list brands - DATA_ENTRY needs this to see added data,
// REVIEWER needs this to review pending data.
@GetMapping
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
public Page<BrandResponse> list(
    @RequestParam(required = false) Boolean published,
    @RequestParam(required = false) Boolean active,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
  Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("priority").ascending());
  return brandService.adminList(published, active, pageable);
}

}
