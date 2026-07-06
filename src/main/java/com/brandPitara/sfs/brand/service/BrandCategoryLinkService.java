package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCategoryLinkResponse;
import com.brandPitara.sfs.brand.dto.BrandCategoryLinkUpsertRequest;

import java.util.List;

public interface BrandCategoryLinkService {

  BrandCategoryLinkResponse upsert(Long brandId, BrandCategoryLinkUpsertRequest request);

  List<BrandCategoryLinkResponse> adminList(Long brandId);

  void softDelete(Long brandId, Long categoryId);
}
