package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCertificateResponse;
import com.brandPitara.sfs.brand.dto.BrandCertificateUpsertRequest;

import java.util.List;

public interface BrandCertificateService {

  BrandCertificateResponse create(Long brandId, BrandCertificateUpsertRequest request);

  BrandCertificateResponse update(Long brandId, Long certificateId, BrandCertificateUpsertRequest request);

  List<BrandCertificateResponse> adminList(Long brandId);

  void softDelete(Long brandId, Long certificateId);
}
