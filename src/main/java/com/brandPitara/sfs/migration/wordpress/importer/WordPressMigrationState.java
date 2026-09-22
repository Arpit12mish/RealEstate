package com.brandPitara.sfs.migration.wordpress.importer;

/**
 * The outcome of importing one WordPress post, distinct from {@code ContentStatus} - a
 * source-published post that only reached {@code ContentStatus.DRAFT} because it needs manual
 * review is recorded here as {@code NEEDS_REVIEW}, not {@code DRAFT}, so provenance tracking can
 * tell the two apart even though the underlying content_post row looks the same either way.
 */
public enum WordPressMigrationState {
    PUBLISHED,
    DRAFT,
    NEEDS_REVIEW,
    BLOCKED,
    FAILED
}
