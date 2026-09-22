package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.WordPressMigrationProperties;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressDumpReader;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import com.brandPitara.sfs.migration.wordpress.gutenberg.UnsupportedBlockReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates a full read-only audit of every editorial WordPress post: streams the dump into a
 * {@link WordPressDataset}, converts each post's Gutenberg content, classifies it, and produces
 * an {@link AuditResult} - no PostgreSQL writes, no S3 calls, nothing extracted from
 * {@code uploads.zip} beyond its entry listing. Never invoked automatically; see
 * {@link WordPressAuditCli}.
 */
public final class WordPressAuditRunner {

    /** The four WordPress post ids this migration must explicitly inspect for Elementor markers. */
    private static final Set<Long> ELEMENTOR_INSPECTION_IDS = Set.of(597L, 640L, 8886L, 9923L, 513L);
    private static final List<String> RANK_MATH_KEYS = List.of(
            "rank_math_title", "rank_math_description", "rank_math_focus_keyword", "rank_math_primary_category"
    );

    private final GutenbergDocumentConverter converter = new GutenbergDocumentConverter();
    private final WordPressMediaReferenceAnalyzer mediaAnalyzer = new WordPressMediaReferenceAnalyzer();
    private final WordPressMigrationProperties authorProperties = new WordPressMigrationProperties();

    public record AuditResult(
            AuditSummary summary,
            List<PostAuditEntry> posts,
            Map<Long, GutenbergConversionResult> conversions
    ) {
    }

    public AuditResult audit(Path dumpPath) throws IOException {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        new WordPressDumpReader().read(dumpPath, builder);
        return audit(builder.build());
    }

    public AuditResult audit(WordPressDataset dataset) {
        List<WordPressPostRow> targetPosts = new ArrayList<>();
        for (WordPressPostRow post : dataset.postsById().values()) {
            if ("post".equals(post.postType()) && ("publish".equals(post.postStatus()) || "draft".equals(post.postStatus()))) {
                targetPosts.add(post);
            }
        }

        List<PostAuditEntry> entries = new ArrayList<>();
        Map<Long, GutenbergConversionResult> conversions = new LinkedHashMap<>();
        Map<Long, Long> authorCounts = new LinkedHashMap<>();
        Map<PostClassification, Integer> classificationCounts = new LinkedHashMap<>();
        Map<String, Map<PostClassification, Integer>> classificationCountsByStatus = new LinkedHashMap<>();
        classificationCountsByStatus.put("publish", zeroedClassificationMap());
        classificationCountsByStatus.put("draft", zeroedClassificationMap());
        for (PostClassification classification : PostClassification.values()) {
            classificationCounts.put(classification, 0);
        }
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Integer> rankMathCounts = new LinkedHashMap<>();
        for (String key : RANK_MATH_KEYS) {
            rankMathCounts.put(key, 0);
        }

        int published = 0, drafts = 0, publishedInterviews = 0, ratingWidgetPosts = 0,
                blankSlugDrafts = 0, emptyExcerpts = 0, featuredImageLinks = 0;
        int resolvableGalleryPosts = 0;
        List<Long> unresolvedGalleryPostIds = new ArrayList<>();

        for (WordPressPostRow post : targetPosts) {
            GutenbergConversionResult conversion = converter.convert(post.id(), post.postTitle(), post.postContent());
            conversions.put(post.id(), conversion);
            PostClassification classification = WordPressPostClassifier.classify(conversion);
            classificationCounts.merge(classification, 1, Integer::sum);

            List<String> categories = dataset.categoriesFor(post.id());
            List<String> tags = dataset.tagsFor(post.id());
            boolean isPublished = "publish".equals(post.postStatus());
            classificationCountsByStatus.get(post.postStatus()).merge(classification, 1, Integer::sum);
            boolean isInterview = categories.contains("Interview");
            boolean hasFeaturedImage = dataset.metaValue(post.id(), "_thumbnail_id") != null;
            boolean blankSlug = post.postName() == null || post.postName().isBlank();
            boolean emptyExcerpt = post.postExcerpt() == null || post.postExcerpt().isBlank();
            boolean hasResolvedGallery = conversion.document().blocks().stream()
                    .anyMatch(b -> b instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Gallery);
            boolean hasUnresolvedGallery = !conversion.unresolvedGalleries().isEmpty();
            boolean hasRatingWidget = conversion.warnings().stream()
                    .anyMatch(w -> "feedbackwp/rating-widget".equals(w.blockName()));

            if (isPublished) {
                published++;
                if (isInterview) {
                    publishedInterviews++;
                }
            } else {
                drafts++;
            }
            if (hasResolvedGallery) {
                resolvableGalleryPosts++;
            }
            if (hasUnresolvedGallery) {
                unresolvedGalleryPostIds.add(post.id());
            }
            if (hasRatingWidget) {
                ratingWidgetPosts++;
            }
            if (blankSlug && !isPublished) {
                blankSlugDrafts++;
            }
            if (emptyExcerpt) {
                emptyExcerpts++;
            }
            if (hasFeaturedImage) {
                featuredImageLinks++;
            }
            if (post.postAuthor() != null) {
                authorCounts.merge(post.postAuthor(), 1L, Long::sum);
            }
            for (String category : categories) {
                categoryCounts.merge(category, 1, Integer::sum);
            }
            for (String key : RANK_MATH_KEYS) {
                String value = dataset.metaValue(post.id(), key);
                if (value != null && !value.isBlank()) {
                    rankMathCounts.merge(key, 1, Integer::sum);
                }
            }

            int unresolvedMediaCount = conversion.referencedExternalUrls().size();
            boolean meaningfulContentLost = meaningfulContentLost(post, conversion, classification);
            String reason = describeReason(post, conversion, classification);
            String remediation = describeRemediation(post, conversion, classification);

            entries.add(new PostAuditEntry(
                    post.id(), post.postTitle(), post.postName(), post.postStatus(),
                    post.postAuthor() == null ? -1 : post.postAuthor(), authorDisplayName(post, dataset),
                    categories, tags, isInterview ? "INTERVIEW" : "BLOG",
                    hasFeaturedImage, blankSlug, emptyExcerpt, classification,
                    conversion.warnings().size(), conversion.blockingErrors().size(),
                    conversion.unsupportedBlocks().size(), unresolvedMediaCount, meaningfulContentLost,
                    reason, remediation, conversion.manualReviewRequired(),
                    conversion.eligibleForAutomaticPublication() && isPublished
            ));
        }

        int galleryBlockPosts = resolvableGalleryPosts + unresolvedGalleryPostIds.size();

        List<ElementorPostReport> elementorReports = new ArrayList<>();
        int elementorAnyMetadataCount = 0, elementorEditModeOrDataKeyCount = 0,
                elementorEditModeBuilderCount = 0, elementorMeaningfulPayloadCount = 0;
        for (Long id : ELEMENTOR_INSPECTION_IDS) {
            WordPressPostRow post = dataset.postsById().get(id);
            if (post == null) {
                continue;
            }
            GutenbergConversionResult conversion = conversions.computeIfAbsent(
                    id, ignored -> converter.convert(post.id(), post.postTitle(), post.postContent())
            );
            boolean hasGutenberg = post.postContent() != null && post.postContent().contains("<!-- wp:");
            boolean hasEditModeKey = dataset.metaFor(id).stream().anyMatch(m -> "_elementor_edit_mode".equals(m.metaKey()));
            boolean hasDataKey = dataset.metaFor(id).stream().anyMatch(m -> "_elementor_data".equals(m.metaKey()));
            boolean hasAnyElementorKey = dataset.metaFor(id).stream()
                    .anyMatch(m -> m.metaKey() != null && m.metaKey().toLowerCase().contains("elementor"));
            String editMode = dataset.metaValue(id, "_elementor_edit_mode");
            String dataPayload = dataset.metaValue(id, "_elementor_data");
            String elementorVersion = dataset.metaValue(id, "_elementor_version");
            boolean editModeOrDataKeyPresent = hasEditModeKey || hasDataKey;
            boolean editModeIsBuilder = "builder".equals(editMode);
            boolean meaningfulPayload = isMeaningfulElementorPayload(dataPayload);
            boolean meaningfulExtracted = !conversion.document().blocks().isEmpty();

            if (hasAnyElementorKey) elementorAnyMetadataCount++;
            if (editModeOrDataKeyPresent) elementorEditModeOrDataKeyCount++;
            if (editModeIsBuilder) elementorEditModeBuilderCount++;
            if (meaningfulPayload) elementorMeaningfulPayloadCount++;

            elementorReports.add(new ElementorPostReport(
                    id, post.postTitle(), hasGutenberg, hasAnyElementorKey, editModeOrDataKeyPresent,
                    editModeIsBuilder, meaningfulPayload, elementorVersion, meaningfulExtracted,
                    WordPressPostClassifier.classify(conversion), conversion.manualReviewRequired()
            ));
        }

        MediaReferenceReconciliation mediaReconciliation = mediaAnalyzer.analyze(targetPosts, dataset);

        AuditSummary summary = new AuditSummary(
                targetPosts.size(), published, drafts, authorCounts, publishedInterviews,
                galleryBlockPosts, resolvableGalleryPosts, unresolvedGalleryPostIds.size(),
                List.copyOf(unresolvedGalleryPostIds), ratingWidgetPosts,
                elementorAnyMetadataCount, elementorEditModeOrDataKeyCount, elementorEditModeBuilderCount,
                elementorMeaningfulPayloadCount, blankSlugDrafts, emptyExcerpts, featuredImageLinks,
                classificationCounts, classificationCountsByStatus, mediaReconciliation, elementorReports,
                categoryCounts, rankMathCounts
        );
        return new AuditResult(summary, entries, conversions);
    }

    private Map<PostClassification, Integer> zeroedClassificationMap() {
        Map<PostClassification, Integer> map = new LinkedHashMap<>();
        for (PostClassification classification : PostClassification.values()) {
            map.put(classification, 0);
        }
        return map;
    }

    /** Empty string, blank, or a JSON "[]" empty array never count as a meaningful payload. */
    private boolean isMeaningfulElementorPayload(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.equals("[]") && !trimmed.equals("a:0:{}");
    }

    private boolean meaningfulContentLost(WordPressPostRow post, GutenbergConversionResult conversion, PostClassification classification) {
        return switch (classification) {
            case BLOCKED -> post.postContent() != null && !post.postContent().isBlank();
            case MANUAL_REVIEW_REQUIRED -> conversion.unsupportedBlocks().stream().anyMatch(UnsupportedBlockReport::hadMeaningfulContent)
                    || !conversion.unresolvedGalleries().isEmpty();
            default -> false;
        };
    }

    private String describeReason(WordPressPostRow post, GutenbergConversionResult conversion, PostClassification classification) {
        if (classification == PostClassification.BLOCKED) {
            if (!conversion.blockingErrors().isEmpty()) {
                return "CMS validator rejected the converted document: " + conversion.blockingErrors().get(0).message();
            }
            boolean rawBlank = post.postContent() == null || post.postContent().isBlank();
            return rawBlank
                    ? "Source post_content is empty (intentionally empty draft)."
                    : "All source content was unsupported or unresolved; nothing survived conversion.";
        }
        if (classification == PostClassification.MANUAL_REVIEW_REQUIRED) {
            List<String> parts = new ArrayList<>();
            for (UnsupportedBlockReport block : conversion.unsupportedBlocks()) {
                // Skip a bare "core/gallery at [path]" here when the same path already gets a
                // more precise entry from unresolvedGalleries below - avoids reporting the same
                // gallery twice with two different levels of detail.
                if (block.disposition() == UnsupportedBlockReport.Disposition.MANUAL_REVIEW_REQUIRED
                        && !"core/gallery".equals(block.blockName())) {
                    parts.add(block.blockName() + " at " + block.path());
                }
            }
            for (var gallery : conversion.unresolvedGalleries()) {
                if (gallery.fullyUnresolved()) {
                    parts.add("core/gallery at " + gallery.path() + " fully unresolved ("
                            + gallery.unresolvedImageUrls().size() + " external image(s), no attachment ID)");
                }
            }
            return parts.isEmpty() ? "Flagged for manual review." : String.join("; ", parts);
        }
        return "";
    }

    private String describeRemediation(WordPressPostRow post, GutenbergConversionResult conversion, PostClassification classification) {
        if (classification == PostClassification.BLOCKED) {
            boolean rawBlank = post.postContent() == null || post.postContent().isBlank();
            if (rawBlank) {
                return "No action needed - leave as draft.";
            }
            if (!conversion.blockingErrors().isEmpty()) {
                return "Review the validator rejection reason and adjust source content, then re-run conversion.";
            }
            return "Manually author replacement content - nothing automatically convertible was found.";
        }
        if (classification == PostClassification.MANUAL_REVIEW_REQUIRED) {
            if (!conversion.unresolvedGalleries().isEmpty()) {
                int externalCount = conversion.unresolvedGalleries().stream()
                        .mapToInt(g -> g.unresolvedImageUrls().size()).sum();
                return "Acquire/upload the " + externalCount + " external gallery image(s) as CMS media, then re-run conversion.";
            }
            if (conversion.unsupportedBlocks().stream().anyMatch(b -> "qligg/box".equals(b.blockName()))) {
                return "Manually rewrite the qligg/box content as native blocks.";
            }
            return "Manually review and rewrite the flagged unsupported block(s) as native blocks.";
        }
        return "";
    }

    private String authorDisplayName(WordPressPostRow post, WordPressDataset dataset) {
        if (post.postAuthor() == null) {
            return null;
        }
        var user = dataset.userById(post.postAuthor());
        return user != null ? user.displayName() : null;
    }

    public Set<Long> configuredAuthorIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (WordPressMigrationProperties.AuthorMapping mapping : authorProperties.getAuthors()) {
            ids.add(mapping.getWordPressAuthorId());
        }
        return ids;
    }
}
