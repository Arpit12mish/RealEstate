package com.brandPitara.sfs.builder.controller.publicapi;

import com.brandPitara.sfs.brand.dto.PublicConnectedBrandsSectionResponse;
import com.brandPitara.sfs.brand.service.BrandConnectedPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderConnectedBrandsPublicController {

  private final BrandConnectedPublicService brandConnectedPublicService;

  @GetMapping("/{builderId}/connected-brands")
  public PublicConnectedBrandsSectionResponse getConnectedBrands(
      @PathVariable Long builderId,
      @RequestParam(defaultValue = "10") int limit
  ) {
    return brandConnectedPublicService.getBuilderConnectedBrands(builderId, limit);
  }
}
