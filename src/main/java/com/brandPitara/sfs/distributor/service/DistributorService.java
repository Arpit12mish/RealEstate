package com.brandPitara.sfs.distributor.service;

import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import com.brandPitara.sfs.distributor.dto.DistributorProfileResponse;
import com.brandPitara.sfs.distributor.dto.DistributorResponse;
import com.brandPitara.sfs.distributor.dto.DistributorUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DistributorService {

  // Admin
  DistributorResponse create(DistributorUpsertRequest request);
  DistributorResponse update(Long id, DistributorUpsertRequest request);
  DistributorResponse getById(Long id);
  void softDelete(Long id);
  Page<DistributorResponse> adminList(Long cityId, Boolean active, Pageable pageable);

  // Public
  Page<DistributorCardResponse> publicListByBrand(Long brandId, Long cityId, Pageable pageable);
  DistributorResponse publicGetDistributor(Long distributorId);
  DistributorProfileResponse publicGetProfile(Long distributorId);

}
