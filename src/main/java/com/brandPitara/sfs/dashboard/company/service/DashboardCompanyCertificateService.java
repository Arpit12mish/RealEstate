package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyCertificateUpdateRequest;

import java.util.List;

public interface DashboardCompanyCertificateService {

  List<CompanyCertificateResponse> list(Long companyId);

  CompanyCertificateResponse create(Long companyId, CompanyCertificateCreateRequest request);

  CompanyCertificateResponse update(Long companyId, Long certificateId, CompanyCertificateUpdateRequest request);

  void delete(Long companyId, Long certificateId);

  List<CompanyCertificateResponse> reorder(Long companyId, CompanyCertificateReorderRequest request);
}
