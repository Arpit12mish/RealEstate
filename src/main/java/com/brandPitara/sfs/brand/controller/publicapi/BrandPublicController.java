package com.brandPitara.sfs.brand.controller.publicapi;

import com.brandPitara.sfs.brand.dto.BrandDetailPageResponse;
import com.brandPitara.sfs.brand.dto.BrandPromoResponse;
import com.brandPitara.sfs.brand.dto.BrandResponse;
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

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandPublicController {

  private final BrandService brandService;

  @GetMapping("/{id}")
  public BrandResponse getById(@PathVariable Long id) {
    return brandService.getById(id);
  }

  @GetMapping
  public Page<BrandResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    int pageSize = Math.min(size, 50);

    Pageable pageable = PageRequest.of(
        page,
        pageSize,
        Sort.by(Sort.Direction.ASC, "priority")  // important for homepage ordering
    );

    return brandService.listPublished(pageable);
  }

  @GetMapping("/{id}/promo")
  public BrandPromoResponse getPromo(@PathVariable Long id) {
    return brandService.getPromo(id);
  }

  @GetMapping("/{id}/page")
  public BrandDetailPageResponse getBrandPage(
      @PathVariable Long id,
      @RequestParam(required = false) Long cityId
  ) {
    return brandService.getBrandPage(id, cityId);
  }


}
