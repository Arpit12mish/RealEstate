package com.brandPitara.sfs.distributor.controller.publicapi;

import com.brandPitara.sfs.distributor.dto.DistributorProfileResponse;
import com.brandPitara.sfs.distributor.dto.DistributorResponse;
import com.brandPitara.sfs.distributor.service.DistributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
public class DistributorPublicController {

  private final DistributorService distributorService;

  @GetMapping("/{id}")
  public DistributorResponse getDistributor(@PathVariable Long id) {
    return distributorService.publicGetDistributor(id);
  }

  @GetMapping("/{id}/profile")
  public DistributorProfileResponse getProfile(@PathVariable Long id) {
    return distributorService.publicGetProfile(id);
  }
}
