package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandFaqResponse;
import com.brandPitara.sfs.brand.dto.BrandFaqUpsertRequest;
import com.brandPitara.sfs.brand.service.BrandFaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandFaqController {

  private final BrandFaqService brandFaqService;

  @PostMapping("/{brandId}/faqs")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandFaqResponse create(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandFaqUpsertRequest request
  ) {
    return brandFaqService.create(brandId, request);
  }

  @PutMapping("/{brandId}/faqs/{faqId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BrandFaqResponse update(
      @PathVariable Long brandId,
      @PathVariable Long faqId,
      @Valid @RequestBody BrandFaqUpsertRequest request
  ) {
    return brandFaqService.update(brandId, faqId, request);
  }

  @GetMapping("/{brandId}/faqs")
  @PreAuthorize("hasRole('ADMIN')")
  public List<BrandFaqResponse> list(@PathVariable Long brandId) {
    return brandFaqService.adminList(brandId);
  }

  @DeleteMapping("/{brandId}/faqs/{faqId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long brandId, @PathVariable Long faqId) {
    brandFaqService.softDelete(brandId, faqId);
  }
}
