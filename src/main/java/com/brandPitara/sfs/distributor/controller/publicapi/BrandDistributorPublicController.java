package com.brandPitara.sfs.distributor.controller.publicapi;

import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import com.brandPitara.sfs.distributor.dto.DistributorResponse;
import com.brandPitara.sfs.distributor.service.DistributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandDistributorPublicController {

  private final DistributorService distributorService;

  @GetMapping("/{brandId}/distributors")
  public Page<DistributorCardResponse> listBrandDistributors(
      @PathVariable Long brandId,
      @RequestParam(required = false) Long cityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50));
    return distributorService.publicListByBrand(brandId, cityId, pageable);
  }
}
