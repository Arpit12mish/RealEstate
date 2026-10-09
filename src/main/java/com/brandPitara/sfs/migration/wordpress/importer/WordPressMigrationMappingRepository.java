package com.brandPitara.sfs.migration.wordpress.importer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordPressMigrationMappingRepository extends JpaRepository<WordPressMigrationMappingEntity, Long> {

    Optional<WordPressMigrationMappingEntity> findBySourceSystemAndSourcePostId(String sourceSystem, Long sourcePostId);

    /** Used by the mapping-backfill tool to refuse pairing two different WordPress source posts to
     * the same already-existing content_post row. */
    Optional<WordPressMigrationMappingEntity> findByTargetContentId(Long targetContentId);
}
