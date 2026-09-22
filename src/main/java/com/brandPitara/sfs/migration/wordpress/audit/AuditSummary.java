package com.brandPitara.sfs.migration.wordpress.audit;

import java.util.List;
import java.util.Map;

public record AuditSummary(
        int totalPosts,
        int published,
        int drafts,
        Map<Long, Long> authorPostCounts,
        int publishedInterviews,
        int galleryBlockPosts,
        int resolvableGalleryPosts,
        int unresolvedGalleryPosts,
        List<Long> unresolvedGalleryPostIds,
        int ratingWidgetPosts,
        int elementorAnyMetadataCount,
        int elementorEditModeOrDataKeyCount,
        int elementorEditModeBuilderCount,
        int elementorMeaningfulPayloadCount,
        int blankSlugDrafts,
        int emptyExcerpts,
        int featuredImageLinks,
        Map<PostClassification, Integer> classificationCounts,
        Map<String, Map<PostClassification, Integer>> classificationCountsByStatus,
        MediaReferenceReconciliation mediaReconciliation,
        List<ElementorPostReport> elementorPosts,
        Map<String, Integer> categoryCounts,
        Map<String, Integer> rankMathFieldCounts
) {
}
