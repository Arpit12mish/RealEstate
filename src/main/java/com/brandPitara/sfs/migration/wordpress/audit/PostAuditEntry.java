package com.brandPitara.sfs.migration.wordpress.audit;

import java.util.List;

public record PostAuditEntry(
        long wordPressPostId,
        String title,
        String slug,
        String status,
        long authorId,
        String authorDisplayName,
        List<String> categories,
        List<String> tags,
        String targetContentType,
        boolean hasFeaturedImage,
        boolean blankSlug,
        boolean emptyExcerpt,
        PostClassification classification,
        int warningCount,
        int blockingErrorCount,
        int unsupportedBlockCount,
        int unresolvedMediaCount,
        boolean meaningfulContentLost,
        String reason,
        String remediation,
        boolean manualReviewRequired,
        boolean eligibleForAutomaticPublication
) {
}
