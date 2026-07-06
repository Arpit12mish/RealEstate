package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandSkuResponse;
import com.brandPitara.sfs.brand.dto.BrandSkuUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandSkuService {

  BrandSkuResponse create(Long brandId, BrandSkuUpsertRequest request);

  BrandSkuResponse update(Long brandId, Long skuId, BrandSkuUpsertRequest request);

  Page<BrandSkuResponse> adminList(Long brandId, Pageable pageable);

  BrandSkuResponse adminGetById(Long brandId, Long skuId);

  void softDelete(Long brandId, Long skuId);
}
