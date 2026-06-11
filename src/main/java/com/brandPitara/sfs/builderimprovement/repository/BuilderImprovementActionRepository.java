package com.brandPitara.sfs.builderimprovement.repository;

import com.brandPitara.sfs.builderimprovement.entity.BuilderImprovementActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderImprovementActionRepository extends JpaRepository<BuilderImprovementActionEntity, Long> {

    Optional<BuilderImprovementActionEntity> findByIdAndProfile_IdAndDeletedFalse(
            Long id,
            Long profileId
    );

    List<BuilderImprovementActionEntity> findByProfile_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );

    List<BuilderImprovementActionEntity>
    findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );
}