package com.brandPitara.sfs.migration.wordpress.importer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordPressMigrationMappingRepository extends JpaRepository<WordPressMigrationMappingEntity, Long> {

    Optional<WordPressMigrationMappingEntity> findBySourceSystemAndSourcePostId(String sourceSystem, Long sourcePostId);
}
