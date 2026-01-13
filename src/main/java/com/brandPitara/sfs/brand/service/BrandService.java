package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {

  // Admin
  BrandResponse create(BrandUpsertRequest request);

  BrandResponse update(Long id, BrandUpsertRequest request);

  BrandResponse setPublished(Long id, boolean published);

  void softDelete(Long id);

  BrandResponse getById(Long id);

  Page<BrandResponse> adminList(Boolean published, Boolean active, Pageable pageable);

  // Public
  Page<BrandResponse> listPublished(Pageable pageable);
}
