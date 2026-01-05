package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.ProviderMediaListResponse;
import com.brandPitara.sfs.provider.dto.ProviderMediaResponse;

public interface ProviderMediaService {

    ProviderMediaListResponse getMyMedia(Long currentUserId);

    ProviderMediaResponse upsertMyProfilePhoto(Long currentUserId, String url, String thumbnailUrl);

    ProviderMediaResponse addMyGalleryImage(Long currentUserId, String url, String thumbnailUrl, int sortOrder);

    void deleteMyMedia(Long currentUserId, Long mediaId);
}
