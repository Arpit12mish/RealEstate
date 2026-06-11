package com.brandPitara.sfs.builderimprovement.repository;

import com.brandPitara.sfs.builderimprovement.entity.BuilderImprovementProfileEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderImprovementProfileRepository extends JpaRepository<BuilderImprovementProfileEntity, Long> {

    Optional<BuilderImprovementProfileEntity> findByIdAndDeletedFalse(Long id);

    Optional<BuilderImprovementProfileEntity>
    findByBuilder_IdAndProjectIsNullAndDeletedFalse(Long builderId);

    Optional<BuilderImprovementProfileEntity>
    findByBuilder_IdAndProject_IdAndDeletedFalse(Long builderId, Long projectId);

    Optional<BuilderImprovementProfileEntity>
    findByBuilder_IdAndProjectIsNullAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
            Long builderId,
            ReviewStatus reviewStatus
    );

    Optional<BuilderImprovementProfileEntity>
    findByBuilder_IdAndProject_IdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
            Long builderId,
            Long projectId,
            ReviewStatus reviewStatus
    );

    boolean existsByBuilder_IdAndProjectIsNullAndDeletedFalse(Long builderId);

    boolean existsByBuilder_IdAndProject_IdAndDeletedFalse(Long builderId, Long projectId);

    List<BuilderImprovementProfileEntity>
    findByBuilder_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long builderId);
}