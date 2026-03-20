package com.brandPitara.sfs.company.service;

import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;

public interface ArchitectDesignerPublicService {
  ArchitectDesignerDetailResponse getDetail(Long companyId);
}