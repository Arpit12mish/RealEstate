package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandUpdateRequest;

import java.util.List;

public interface DashboardCompanyConnectedBrandService {

  List<CompanyConnectedBrandResponse> list(Long companyId);

  CompanyConnectedBrandResponse create(Long companyId, CompanyConnectedBrandCreateRequest request);

  CompanyConnectedBrandResponse update(
      Long companyId,
      Long collaborationId,
      CompanyConnectedBrandUpdateRequest request
  );

  void delete(Long companyId, Long collaborationId);
}
