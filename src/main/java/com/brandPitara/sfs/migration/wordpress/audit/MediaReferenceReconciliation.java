package com.brandPitara.sfs.migration.wordpress.audit;

/**
 * The image-reference discrepancy from the discovery report, reconciled into precisely defined,
 * non-interchangeable categories rather than picking whichever number happens to match a prior
 * report. Each field is measured over the same fixed post set (the 144 editorial posts) but at a
 * different point in a normalize/dedupe/match pipeline - see
 * {@link WordPressMediaReferenceAnalyzer} for exactly how each is computed.
 */
public record MediaReferenceReconciliation(
        int rawImageOccurrences,
        int uniqueOriginalUrls,
        int normalizedUniqueUrls,
        int ownDomainUrls,
        int externalUrls,
        int attachmentBackedReferences,
        int unresolvedReferences
) {
    public String explanation() {
        return """
                rawImageOccurrences (%d): every <img src="..."> match across all 144 posts' post_content, \
                counted once per occurrence - the same physical photo used twice in one post counts twice.
                uniqueOriginalUrls (%d): distinct URL strings as literally written (case-sensitive, exact), \
                including every WordPress size-variant suffix (e.g. "-1024x576") as a DIFFERENT url.
                normalizedUniqueUrls (%d): uniqueOriginalUrls after stripping the "-WIDTHxHEIGHT" size-variant \
                suffix, so a photo referenced at three different sizes collapses to one entry. This is the \
                figure closest in spirit to the discovery report's "379 unique inline image URLs", though the \
                exact number depends on the normalization rule above and will not equal 379 exactly unless \
                that same rule was used originally.
                ownDomainUrls (%d) + externalUrls (%d) partition normalizedUniqueUrls by host - this is where \
                the earlier "375 + 8 = 383" figure came from, and it will only equal normalizedUniqueUrls if \
                every normalized URL falls cleanly into exactly one of the two buckets (it does, by \
                construction: every URL either resolves to squarefootstory.com or it does not).
                attachmentBackedReferences (%d): of ownDomainUrls, how many match a real wp_posts \
                (post_type=attachment) row's _wp_attached_file value after the same normalization - i.e. a \
                genuine WordPress attachment exists for this image, not just a URL pattern that looks local.
                unresolvedReferences (%d): ownDomainUrls with no matching attachment - broken links, or images \
                predating attachment tracking. attachmentBackedReferences + unresolvedReferences == ownDomainUrls.
                """.formatted(
                rawImageOccurrences, uniqueOriginalUrls, normalizedUniqueUrls,
                ownDomainUrls, externalUrls, attachmentBackedReferences, unresolvedReferences
        );
    }
}
