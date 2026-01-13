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

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public BrandResponse create(@Valid @RequestBody BrandUpsertRequest request) {
    return brandService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandUpsertRequest request) {
    return brandService.update(id, request);
  }

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

  @GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public BrandResponse getById(@PathVariable Long id) {
  return brandService.getById(id);
}

@GetMapping
@PreAuthorize("hasRole('ADMIN')")
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
