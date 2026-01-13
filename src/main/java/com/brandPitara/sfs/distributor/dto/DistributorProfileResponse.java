package com.brandPitara.sfs.distributor.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributorProfileResponse {

  private DistributorResponse distributor;
  private List<DistributorBrandCard> brands;      // each brand with offer info
  private List<DistributorMediaResponse> gallery; // images/videos
}
