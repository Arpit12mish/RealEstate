package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandFaqResponse;
import com.brandPitara.sfs.brand.dto.BrandFaqUpsertRequest;

import java.util.List;

public interface BrandFaqService {

  BrandFaqResponse create(Long brandId, BrandFaqUpsertRequest request);

  BrandFaqResponse update(Long brandId, Long faqId, BrandFaqUpsertRequest request);

  List<BrandFaqResponse> adminList(Long brandId);

  void softDelete(Long brandId, Long faqId);
}
