package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandDistributorResponse;
import com.brandPitara.sfs.brand.dto.BrandDistributorUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandDistributorService {

  BrandDistributorResponse upsert(Long brandId, BrandDistributorUpsertRequest request);

  Page<BrandDistributorResponse> adminList(Long brandId, Pageable pageable);

  void softDelete(Long brandId, Long distributorId);
}
