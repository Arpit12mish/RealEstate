package com.brandPitara.sfs.brand.dto;

import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BrandDetailPageResponse {
  private BrandPublicResponse brand;
  private String offerLine;                    // can be brand.description snippet or derived
  private List<BrandMediaResponse> banners;    // placement=BANNER
  private List<BrandMediaResponse> gallery;    // placement=GALLERY (optional)

  private List<DistributorCardResponse> topDistributors;
  private long distributorCount;
}
