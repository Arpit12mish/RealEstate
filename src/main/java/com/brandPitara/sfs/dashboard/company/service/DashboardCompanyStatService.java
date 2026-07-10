package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyStatCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatUpdateRequest;

import java.util.List;

public interface DashboardCompanyStatService {

  List<CompanyStatResponse> list(Long companyId);

  CompanyStatResponse create(Long companyId, CompanyStatCreateRequest request);

  CompanyStatResponse update(Long companyId, Long statId, CompanyStatUpdateRequest request);

  void delete(Long companyId, Long statId);

  List<CompanyStatResponse> reorder(Long companyId, CompanyStatReorderRequest request);
}
