package com.brandPitara.sfs.cms.metadata.dto;

import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import java.time.OffsetDateTime;

public record CmsCategoryResponse(Long id, String name, String slug, String description, boolean active,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {
    public static CmsCategoryResponse from(CmsContentCategoryEntity c) {
        return new CmsCategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                Boolean.TRUE.equals(c.getActive()), c.getCreatedAt(), c.getUpdatedAt(), c.getVersion());
    }
}
