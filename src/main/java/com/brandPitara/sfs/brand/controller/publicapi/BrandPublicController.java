package com.brandPitara.sfs.brand.controller.publicapi;

import com.brandPitara.sfs.brand.dto.BrandDetailPageResponse;
import com.brandPitara.sfs.brand.dto.BrandPromoResponse;
import com.brandPitara.sfs.brand.dto.BrandPublicResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandCategoryResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandDetailResponse;
import com.brandPitara.sfs.brand.service.BrandPublicService;
import com.brandPitara.sfs.brand.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandPublicController {

  private final BrandService brandService;
  private final BrandPublicService brandPublicService;

  @GetMapping("/categories")
  public List<PublicBrandCategoryResponse> listCategories() {
    return brandPublicService.listPublicCategories();
  }

  @GetMapping
  public Page<PublicBrandCardResponse> list(
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 50),
        Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("id"))
    );

    return brandPublicService.listPublicBrands(categoryId, q, pageable);
  }

  // Legacy, numeric-only - kept for backward compatibility (no known consumer today,
  // but preserving it costs nothing and avoids breaking any internal client, dashboard
  // test, Postman collection, or in-progress mobile integration this repo can't see).
  // New mobile work should use the slug endpoint below exclusively.
  @GetMapping("/{id:\\d+}")
  public BrandPublicResponse getById(@PathVariable Long id) {
    return brandService.getById(id);
  }

  // Canonical detail endpoint going forward. The regex requires at least one alphabetic
  // character, so a purely-numeric path (e.g. "1") can never match this pattern at all -
  // unlike the previous [a-z0-9][a-z0-9-]* constraint, this makes the two routes
  // structurally mutually exclusive instead of relying on Spring's runtime tie-breaking
  // (which, contrary to an earlier isolated PathPattern diagnostic, actually threw
  // "Ambiguous handler methods mapped" for numeric paths in the real running app).
  @GetMapping("/{slug:[a-z0-9-]*[a-z][a-z0-9-]*}")
  public PublicBrandDetailResponse getBySlug(@PathVariable String slug) {
    return brandPublicService.getPublicBrandBySlug(slug);
  }

  @GetMapping("/{id:\\d+}/promo")
  public BrandPromoResponse getPromo(@PathVariable Long id) {
    return brandService.getPromo(id);
  }

  @GetMapping("/{id:\\d+}/page")
  public BrandDetailPageResponse getBrandPage(
      @PathVariable Long id,
      @RequestParam(required = false) Long cityId
  ) {
    return brandService.getBrandPage(id, cityId);
  }
}
