package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;

/**
 * Classifies a converted post into exactly one of four buckets, independent of its WordPress
 * {@code post_status} - a draft can be "conversion-ready" (the content converted cleanly) while
 * still never being eligible for publication (that is a separate, later workflow decision; see
 * {@link com.brandPitara.sfs.migration.wordpress.audit.WordPressAuditRunner}).
 */
public final class WordPressPostClassifier {

    private WordPressPostClassifier() {
    }

    public static PostClassification classify(GutenbergConversionResult result) {
        boolean blocked = result.document().blocks().isEmpty() || !result.blockingErrors().isEmpty();
        if (blocked) {
            return PostClassification.BLOCKED;
        }
        if (result.manualReviewRequired()) {
            return PostClassification.MANUAL_REVIEW_REQUIRED;
        }
        if (!result.warnings().isEmpty()) {
            return PostClassification.CONVERSION_READY_WITH_WARNINGS;
        }
        return PostClassification.CONVERSION_READY;
    }
}
