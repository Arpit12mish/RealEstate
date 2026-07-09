package com.brandPitara.sfs.instagram.service;

import com.brandPitara.sfs.instagram.dto.InstagramCachedAsset;

public interface InstagramAssetCacheService {
    InstagramCachedAsset cacheThumbnail(String instagramMediaId, String sourceThumbnailUrl);
}
