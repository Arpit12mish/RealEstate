package com.brandPitara.sfs.builderimprovement.repository;

import com.brandPitara.sfs.builderimprovement.entity.BuilderImprovementTimelineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderImprovementTimelineRepository extends JpaRepository<BuilderImprovementTimelineEntity, Long> {

    Optional<BuilderImprovementTimelineEntity> findByIdAndProfile_IdAndDeletedFalse(
            Long id,
            Long profileId
    );

    List<BuilderImprovementTimelineEntity> findByProfile_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );

    List<BuilderImprovementTimelineEntity>
    findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );
}