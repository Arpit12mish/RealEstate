package com.brandPitara.sfs.dashboard.company.service;

import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyPricingPlanUpdateRequest;

import java.util.List;

public interface DashboardCompanyPricingPlanService {

  List<CompanyPricingPlanResponse> list(Long companyId);

  CompanyPricingPlanResponse create(Long companyId, CompanyPricingPlanCreateRequest request);

  CompanyPricingPlanResponse update(Long companyId, Long planId, CompanyPricingPlanUpdateRequest request);

  void delete(Long companyId, Long planId);

  List<CompanyPricingPlanResponse> reorder(Long companyId, CompanyPricingPlanReorderRequest request);
}
