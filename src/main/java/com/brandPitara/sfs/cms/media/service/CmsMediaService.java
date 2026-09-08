package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.dto.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;


public interface CmsMediaService {
    CmsMediaUploadResponse createUpload(CmsMediaUploadRequest request, Authentication authentication);
    CmsMediaAssetResponse complete(Long id, Authentication authentication);
    Page<CmsMediaAssetResponse> list(CmsMediaType type, CmsMediaStatus status, Long createdBy,
                                     String search, Pageable pageable);
    CmsMediaAssetResponse get(Long id);
}
