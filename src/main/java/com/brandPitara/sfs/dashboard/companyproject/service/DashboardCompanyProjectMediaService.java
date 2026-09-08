package com.brandPitara.sfs.dashboard.companyproject.service;

import com.brandPitara.sfs.company.dto.CompanyProjectMediaResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.*;

import java.util.List;

public interface DashboardCompanyProjectMediaService {
  List<CompanyProjectMediaResponse> list(Long companyProjectId);
  CompanyProjectMediaResponse create(Long companyProjectId, CompanyProjectMediaCreateRequest request);
  CompanyProjectMediaResponse update(Long companyProjectId, Long mediaId, CompanyProjectMediaUpdateRequest request);
  void delete(Long companyProjectId, Long mediaId);
  List<CompanyProjectMediaResponse> reorder(Long companyProjectId, CompanyProjectMediaReorderRequest request);
}
