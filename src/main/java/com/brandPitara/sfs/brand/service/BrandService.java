package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandDetailPageResponse;
import com.brandPitara.sfs.brand.dto.BrandPromoResponse;
import com.brandPitara.sfs.brand.dto.BrandPublicResponse;
import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {

  // Admin — return full BrandResponse (includes active/published/priority)
  BrandResponse create(BrandUpsertRequest request);

  BrandResponse update(Long id, BrandUpsertRequest request);

  BrandResponse setPublished(Long id, boolean published);

  void softDelete(Long id);

  BrandResponse adminGetById(Long id);

  Page<BrandResponse> adminList(Boolean published, Boolean active, Pageable pageable);

  // Public — return stripped BrandPublicResponse
  BrandPublicResponse getById(Long id);

  Page<BrandPublicResponse> listPublished(Pageable pageable);

  // Public promo
  BrandPromoResponse getPromo(Long id);

  BrandDetailPageResponse getBrandPage(Long brandId, Long cityId);


}
