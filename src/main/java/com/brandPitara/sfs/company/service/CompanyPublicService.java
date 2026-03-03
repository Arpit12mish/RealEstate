package com.brandPitara.sfs.company.service;

import com.brandPitara.sfs.company.dto.CompanyResponse;
import org.springframework.data.domain.*;

public interface CompanyPublicService {
  CompanyResponse publicGet(Long companyId);
  Page<CompanyResponse> publicList(String companyType, Pageable pageable);
}