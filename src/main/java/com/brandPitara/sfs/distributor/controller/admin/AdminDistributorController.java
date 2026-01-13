package com.brandPitara.sfs.distributor.controller.admin;

import com.brandPitara.sfs.distributor.dto.DistributorResponse;
import com.brandPitara.sfs.distributor.dto.DistributorUpsertRequest;
import com.brandPitara.sfs.distributor.service.DistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/distributors")
@RequiredArgsConstructor
public class AdminDistributorController {

  private final DistributorService distributorService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public DistributorResponse create(@Valid @RequestBody DistributorUpsertRequest request) {
    return distributorService.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public DistributorResponse update(@PathVariable Long id, @Valid @RequestBody DistributorUpsertRequest request) {
    return distributorService.update(id, request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public DistributorResponse getById(@PathVariable Long id) {
    return distributorService.getById(id);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Page<DistributorResponse> list(
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("id").descending());
    return distributorService.adminList(cityId, active, pageable);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    distributorService.softDelete(id);
  }
}
