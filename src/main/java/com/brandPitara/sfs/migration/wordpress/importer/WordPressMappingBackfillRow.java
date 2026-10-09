package com.brandPitara.sfs.migration.wordpress.importer;

import java.util.List;

/**
 * One pair's full validation/comparison result - lives in this package (not {@code cli}) because
 * {@link WordPressMappingBackfillValidator} (the only thing that can produce one, since building
 * it requires an open Hibernate session to read {@code ContentPostEntity}'s lazy associations)
 * belongs here alongside the other real Spring beans this backfill tool uses. The {@code cli}
 * package's runner only ever consumes and prints these; it builds none of them itself.
 */
public record WordPressMappingBackfillRow(
        long wordPressPostId, long targetContentPostId,
        String wordPressTitle, String targetTitle,
        String wordPressSlug, String targetSlug,
        String targetStatus,
        String fingerprint,
        IdentityResult identityResult,
        ContentComparison contentComparison,
        PlannedAction plannedAction,
        String blockReason
) {
    public enum IdentityResult { SLUG_EXACT_MATCH, TITLE_NORMALIZED_MATCH, NO_MATCH }

    public enum PlannedAction { INSERT, ADOPT_EXISTING_WITH_DIFFERENCES, ALREADY_MAPPED, BLOCKED }

    public record ContentComparison(
            boolean titleMatches, boolean slugMatches, boolean authorMatches, boolean contentTypeMatches,
            boolean statusMatches, boolean categoriesMatch, boolean tagsMatch, boolean featuredMediaMatches,
            boolean seoMatches, boolean documentEquivalent, List<String> differingFields
    ) {
        public boolean isFullyEquivalent() {
            return differingFields.isEmpty();
        }
    }
}
