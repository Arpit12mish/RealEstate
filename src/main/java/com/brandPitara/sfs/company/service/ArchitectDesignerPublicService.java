package com.brandPitara.sfs.company.service;

import com.brandPitara.sfs.company.dto.ArchitectDesignerDetailResponse;
import com.brandPitara.sfs.company.dto.ArchitectDesignerListItemResponse;
import com.brandPitara.sfs.company.enums.ArchitectDesignerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArchitectDesignerPublicService {
  ArchitectDesignerDetailResponse getDetail(Long companyId);

  // Phase 8A-G (GAP-003B) - canonical slug lookup, scoped to the requested
  // normalized type so a valid slug of the other type never resolves.
  ArchitectDesignerDetailResponse getDetailBySlug(String slug, ArchitectDesignerType type);

  // Phase 8A-G (GAP-037) - the previously-missing public listing contract.
  Page<ArchitectDesignerListItemResponse> list(ArchitectDesignerType type, Pageable pageable);
}
