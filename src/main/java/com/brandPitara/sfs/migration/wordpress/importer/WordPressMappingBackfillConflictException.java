package com.brandPitara.sfs.migration.wordpress.importer;

/**
 * Thrown by {@link WordPressMappingBackfillWriter#applyBackfill} when a re-check immediately
 * before an insert finds a conflict that did not exist when the runner validated the manifest.
 * Unchecked so the surrounding {@code @Transactional} boundary rolls back on it by default,
 * discarding every insert already made in that same apply call.
 */
public class WordPressMappingBackfillConflictException extends RuntimeException {

    public WordPressMappingBackfillConflictException(String message) {
        super(message);
    }
}
