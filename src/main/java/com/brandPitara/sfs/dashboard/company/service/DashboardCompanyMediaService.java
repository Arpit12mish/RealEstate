package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaUpdateRequest;

import java.util.List;

public interface DashboardCompanyMediaService {

  List<CompanyMediaResponse> list(Long companyId);

  CompanyMediaResponse create(Long companyId, CompanyMediaCreateRequest request);

  CompanyMediaResponse update(Long companyId, Long mediaId, CompanyMediaUpdateRequest request);

  void delete(Long companyId, Long mediaId);

  List<CompanyMediaResponse> reorder(Long companyId, CompanyMediaReorderRequest request);
}
