package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.publiccontent.dto.PublicContentPageResponse;

public interface PublicContentService {
    PublicContentResult getBySlug(String slug, String ifNoneMatch);

    PublicContentPageResponse list(
            ContentType contentType, String categorySlug, String author, String tag, String q, int page, int size
    );

    default PublicContentPageResponse list(ContentType contentType, int page, int size) {
        return list(contentType, null, null, null, null, page, size);
    }
}
