package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.PresignProviderMediaRequest;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaResponse;

public interface ProviderMediaPresignService {
    PresignProviderMediaResponse createPresignedUpload(Long currentUserId, PresignProviderMediaRequest request);
}
