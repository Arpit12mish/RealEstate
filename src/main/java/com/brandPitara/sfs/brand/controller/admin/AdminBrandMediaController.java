package com.brandPitara.sfs.brand.controller.admin;

import com.brandPitara.sfs.brand.dto.BrandMediaResponse;
import com.brandPitara.sfs.brand.dto.BrandMediaUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import com.brandPitara.sfs.brand.service.BrandMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandMediaController {

  private final BrandMediaService brandMediaService;

  @PostMapping("/{brandId}/media")
  @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
  public BrandMediaResponse addMedia(
      @PathVariable Long brandId,
      @Valid @RequestBody BrandMediaUpsertRequest request
  ) {
    return brandMediaService.addMedia(brandId, request);
  }

  @GetMapping("/{brandId}/media")
  @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
  public List<BrandMediaResponse> list(
      @PathVariable Long brandId,
      @RequestParam Placement placement
  ) {
    return brandMediaService.listMedia(brandId, placement);
  }

  @DeleteMapping("/{brandId}/media/{mediaId}")
  @PreAuthorize("hasAuthority('ADMIN') or hasRole('ADMIN')")
  public void delete(
      @PathVariable Long brandId,
      @PathVariable Long mediaId
  ) {
    brandMediaService.softDeleteMedia(brandId, mediaId);
  }
}
