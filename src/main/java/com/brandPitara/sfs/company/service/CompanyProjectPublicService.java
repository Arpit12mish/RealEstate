package com.brandPitara.sfs.company.service;

import com.brandPitara.sfs.company.dto.*;
import org.springframework.data.domain.*;

public interface CompanyProjectPublicService {
  Page<CompanyProjectCardDto> publicListByCompany(Long companyId, Pageable pageable);
  CompanyProjectResponse publicGet(Long companyProjectId);
}