package com.brandPitara.sfs.cms.metadata.dto;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import java.time.OffsetDateTime;

public record CmsAuthorResponse(Long id, String displayName, String slug, String bio,
                                String designation, Long profileMediaAssetId, boolean active,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {
    public static CmsAuthorResponse from(CmsPublicAuthorEntity a) {
        return new CmsAuthorResponse(a.getId(), a.getDisplayName(), a.getSlug(), a.getBio(),
                a.getDesignation(), a.getProfileMediaAsset() == null ? null : a.getProfileMediaAsset().getId(),
                Boolean.TRUE.equals(a.getActive()), a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
