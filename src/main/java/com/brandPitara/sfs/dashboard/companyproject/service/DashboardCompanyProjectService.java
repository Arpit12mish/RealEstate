package com.brandPitara.sfs.dashboard.companyproject.service;

import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectDetailResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectListItemResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardCompanyProjectService {

  Page<CompanyProjectListItemResponse> list(
      String q,
      Long companyId,
      String companyType,
      Long cityId,
      Boolean active,
      Pageable pageable
  );

  CompanyProjectDetailResponse getDetail(Long companyProjectId);

  CompanyProjectDetailResponse create(CompanyProjectCreateRequest request);

  CompanyProjectDetailResponse update(Long companyProjectId, CompanyProjectUpdateRequest request);

  void softDelete(Long companyProjectId);
}
