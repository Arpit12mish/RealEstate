package com.brandPitara.sfs.migration.wordpress.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordPressMediaMappingRepository extends JpaRepository<WordPressMediaMappingEntity, Long> {

    Optional<WordPressMediaMappingEntity> findBySourceSystemAndWordPressAttachmentId(
            String sourceSystem, Long wordPressAttachmentId
    );
}
