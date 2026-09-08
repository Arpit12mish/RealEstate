package com.brandPitara.sfs.cms.metadata.dto;

import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import java.time.OffsetDateTime;

public record CmsTagResponse(Long id, String name, String slug, boolean active,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {
    public static CmsTagResponse from(CmsContentTagEntity t) {
        return new CmsTagResponse(t.getId(), t.getName(), t.getSlug(), Boolean.TRUE.equals(t.getActive()),
                t.getCreatedAt(), t.getUpdatedAt(), t.getVersion());
    }
}
