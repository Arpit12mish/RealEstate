package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCollaborationResponse;
import com.brandPitara.sfs.brand.dto.BrandCollaborationUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandCollaborationService {

  BrandCollaborationResponse create(Long brandId, BrandCollaborationUpsertRequest request);

  BrandCollaborationResponse update(Long brandId, Long collaborationId, BrandCollaborationUpsertRequest request);

  Page<BrandCollaborationResponse> adminList(Long brandId, Pageable pageable);

  void softDelete(Long brandId, Long collaborationId);
}
