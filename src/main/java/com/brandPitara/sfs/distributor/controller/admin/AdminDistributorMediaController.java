package com.brandPitara.sfs.distributor.controller.admin;

import com.brandPitara.sfs.distributor.dto.DistributorMediaResponse;
import com.brandPitara.sfs.distributor.dto.DistributorMediaUpsertRequest;
import com.brandPitara.sfs.distributor.service.DistributorMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/distributors")
@RequiredArgsConstructor
public class AdminDistributorMediaController {

  private final DistributorMediaService distributorMediaService;

  @PostMapping("/{id}/media")
  @PreAuthorize("hasRole('ADMIN')")
  public DistributorMediaResponse addMedia(
      @PathVariable("id") Long distributorId,
      @Valid @RequestBody DistributorMediaUpsertRequest request
  ) {
    return distributorMediaService.addMedia(distributorId, request);
  }

  @GetMapping("/{id}/media")
  @PreAuthorize("hasRole('ADMIN')")
  public List<DistributorMediaResponse> listMedia(@PathVariable("id") Long distributorId) {
    return distributorMediaService.listMedia(distributorId);
  }

  @DeleteMapping("/{id}/media/{mediaId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteMedia(@PathVariable("id") Long distributorId, @PathVariable Long mediaId) {
    distributorMediaService.softDeleteMedia(distributorId, mediaId);
  }
}
