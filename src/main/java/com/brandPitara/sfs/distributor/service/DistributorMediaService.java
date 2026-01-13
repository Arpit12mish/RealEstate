package com.brandPitara.sfs.distributor.service;

import com.brandPitara.sfs.distributor.dto.DistributorMediaResponse;
import com.brandPitara.sfs.distributor.dto.DistributorMediaUpsertRequest;

import java.util.List;

public interface DistributorMediaService {
  DistributorMediaResponse addMedia(Long distributorId, DistributorMediaUpsertRequest request);
  List<DistributorMediaResponse> listMedia(Long distributorId);
  void softDeleteMedia(Long distributorId, Long mediaId);
}
