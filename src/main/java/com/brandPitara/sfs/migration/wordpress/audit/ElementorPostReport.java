package com.brandPitara.sfs.migration.wordpress.audit;

/**
 * Elementor evidence for one post, broken into four explicit, non-overlapping-by-definition
 * categories rather than one ambiguous "Elementor-marked" flag - a post can have incidental
 * version/template metadata (e.g. post 513) without ever having been authored in Elementor's
 * page builder.
 */
public record ElementorPostReport(
        long wordPressPostId,
        String title,
        boolean hasGutenbergContent,
        boolean hasAnyElementorMetadata,
        boolean hasEditModeOrDataKey,
        boolean editModeIsBuilder,
        boolean hasMeaningfulDataPayload,
        String elementorVersionMarker,
        boolean hasMeaningfulExtractedContent,
        PostClassification conversionResult,
        boolean manualReviewRequired
) {
}
