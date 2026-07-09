package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandProductCategoryResponse;
import com.brandPitara.sfs.brand.dto.BrandProductCategoryUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandProductCategoryService {

  BrandProductCategoryResponse create(Long brandId, BrandProductCategoryUpsertRequest request);

  BrandProductCategoryResponse update(Long brandId, Long categoryId, BrandProductCategoryUpsertRequest request);

  Page<BrandProductCategoryResponse> adminList(Long brandId, Pageable pageable);

  BrandProductCategoryResponse adminGetById(Long brandId, Long categoryId);

  void softDelete(Long brandId, Long categoryId);
}
