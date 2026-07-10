package com.brandPitara.sfs.dashboard.companyproject.service;

import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedReorderRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedUpdateRequest;

import java.util.List;

public interface DashboardCompanyProjectBrandService {

  List<CompanyProjectBrandUsedResponse> list(Long companyProjectId);

  CompanyProjectBrandUsedResponse create(Long companyProjectId, CompanyProjectBrandUsedCreateRequest request);

  CompanyProjectBrandUsedResponse update(
      Long companyProjectId,
      Long collaborationId,
      CompanyProjectBrandUsedUpdateRequest request
  );

  void delete(Long companyProjectId, Long collaborationId);

  List<CompanyProjectBrandUsedResponse> reorder(Long companyProjectId, CompanyProjectBrandUsedReorderRequest request);
}
