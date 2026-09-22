package com.brandPitara.sfs.migration.wordpress.importer;

public record WordPressImportOutcome(
        long sourcePostId,
        WordPressMigrationState migrationState,
        Long targetContentId,
        boolean skippedIdempotent,
        String errorCode
) {
}
