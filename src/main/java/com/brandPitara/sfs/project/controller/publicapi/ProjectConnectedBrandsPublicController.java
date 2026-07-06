package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.brand.dto.PublicConnectedBrandsSectionResponse;
import com.brandPitara.sfs.brand.service.BrandConnectedPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectConnectedBrandsPublicController {

  private final BrandConnectedPublicService brandConnectedPublicService;

  @GetMapping("/{projectId}/connected-brands")
  public PublicConnectedBrandsSectionResponse getConnectedBrands(
      @PathVariable Long projectId,
      @RequestParam(defaultValue = "10") int limit
  ) {
    return brandConnectedPublicService.getProjectConnectedBrands(projectId, limit);
  }
}
