package com.brandPitara.sfs.cms.content.slug;

public interface ContentSlugService {
    String normalize(String value);

    String resolveForCreate(String requestedSlug, String title);

    String resolveForUpdate(String requestedSlug, Long contentId);
}
