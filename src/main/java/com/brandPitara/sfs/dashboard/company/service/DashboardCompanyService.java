package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyDetailResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyListItemResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardCompanyService {

  Page<CompanyListItemResponse> list(
      String q,
      String companyType,
      Long cityId,
      Boolean active,
      Boolean published,
      Pageable pageable
  );

  CompanyDetailResponse getDetail(Long companyId);

  CompanyDetailResponse create(CompanyCreateRequest request);

  CompanyDetailResponse update(Long companyId, CompanyUpdateRequest request);

  void softDelete(Long companyId);
}
