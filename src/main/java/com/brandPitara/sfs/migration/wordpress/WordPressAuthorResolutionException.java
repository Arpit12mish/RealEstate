package com.brandPitara.sfs.migration.wordpress;

/**
 * Thrown when a WordPress post's author cannot be resolved to a configured CMS byline, or when
 * the resolved per-author post counts don't reconcile with the configured expectations. Never
 * caught to silently fall back to a default author - every one of the migrated posts must have
 * an explicit, resolved byline.
 */
public class WordPressAuthorResolutionException extends RuntimeException {

    public WordPressAuthorResolutionException(String message) {
        super(message);
    }

    public static WordPressAuthorResolutionException unknownAuthor(long wordPressAuthorId) {
        return new WordPressAuthorResolutionException(
                "No configured public-author mapping for WordPress author ID " + wordPressAuthorId
                        + ". Add an entry under sfs.migration.wordpress.authors before importing this post."
        );
    }

    public static WordPressAuthorResolutionException postCountMismatch(
            long wordPressAuthorId, String displayName, long expected, long actual
    ) {
        return new WordPressAuthorResolutionException(
                "WordPress author " + wordPressAuthorId + " (" + displayName + ") was expected to have "
                        + expected + " posts but the source data has " + actual + "."
        );
    }

    public static WordPressAuthorResolutionException totalMismatch(long expectedTotal, long actualTotal) {
        return new WordPressAuthorResolutionException(
                "Author reconciliation expected " + expectedTotal + " total posts across all configured "
                        + "authors but the source data has " + actualTotal + "."
        );
    }

    public static WordPressAuthorResolutionException authorIdentityConflict(
            long wordPressAuthorId, String expectedDisplayName, Long existingAuthorId, String existingDisplayName
    ) {
        return new WordPressAuthorResolutionException(
                "WordPress author " + wordPressAuthorId + " maps to display name '" + expectedDisplayName
                        + "', but an existing CMS public author (id=" + existingAuthorId + ") with the same slug "
                        + "has display name '" + existingDisplayName + "'. This is a blocking identity conflict - "
                        + "resolve it manually (rename one of the two) before importing this author's posts."
        );
    }
}
