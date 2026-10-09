package com.brandPitara.sfs.migration.wordpress.importer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The only component allowed to write {@code wordpress_migration_mapping} on behalf of the
 * mapping-backfill tool - deliberately a separate {@code @Service} bean (not a method on the
 * runner) so {@link #applyBackfill} is a real Spring-proxied {@code @Transactional} boundary: if
 * any pending insert conflicts with state that appeared since the runner's own dry-run-equivalent
 * validation, the exception this throws rolls back every insert already made in this same call,
 * not just the one that failed. Touches no other table, no S3, no {@code content_post}.
 */
@Service
@RequiredArgsConstructor
public class WordPressMappingBackfillWriter {

    private static final String SOURCE_SYSTEM = "wordpress";

    private final WordPressMigrationMappingRepository mappingRepository;

    public record PendingInsert(long wordPressPostId, long targetContentId, String fingerprint) {
    }

    /**
     * Re-checks each pending insert against the database immediately before writing it (the
     * runner's own validation may be milliseconds to minutes old) and inserts all of them in one
     * transaction. Any single conflict - a source or target mapping that now exists - throws
     * {@link WordPressMappingBackfillConflictException}, rolling back every insert this call has
     * made so far. Never called with a row whose planned action was {@code ALREADY_MAPPED} or
     * {@code BLOCKED} - the runner filters those out before calling this.
     */
    @Transactional
    public void applyBackfill(List<PendingInsert> pendingInserts) {
        for (PendingInsert insert : pendingInserts) {
            if (mappingRepository.findBySourceSystemAndSourcePostId(SOURCE_SYSTEM, insert.wordPressPostId()).isPresent()) {
                throw new WordPressMappingBackfillConflictException(
                        "WordPress post " + insert.wordPressPostId() + " already has a mapping row - "
                                + "it appeared after validation ran. Aborting; no partial backfill is written.");
            }
            if (mappingRepository.findByTargetContentId(insert.targetContentId()).isPresent()) {
                throw new WordPressMappingBackfillConflictException(
                        "content_post " + insert.targetContentId() + " is already mapped from another "
                                + "WordPress post - it appeared after validation ran. Aborting; no partial "
                                + "backfill is written.");
            }
            mappingRepository.save(WordPressMigrationMappingEntity.builder()
                    .sourceSystem(SOURCE_SYSTEM)
                    .sourcePostId(insert.wordPressPostId())
                    .targetContentId(insert.targetContentId())
                    .sourceFingerprint(insert.fingerprint())
                    .migrationState(WordPressMigrationState.PUBLISHED)
                    .build());
        }
    }
}
