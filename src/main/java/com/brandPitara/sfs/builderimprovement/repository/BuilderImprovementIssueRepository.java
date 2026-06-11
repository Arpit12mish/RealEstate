package com.brandPitara.sfs.builderimprovement.repository;

import com.brandPitara.sfs.builderimprovement.entity.BuilderImprovementIssueEntity;
import com.brandPitara.sfs.builderimprovement.enums.BuilderImprovementIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderImprovementIssueRepository extends JpaRepository<BuilderImprovementIssueEntity, Long> {

    Optional<BuilderImprovementIssueEntity> findByIdAndProfile_IdAndDeletedFalse(
            Long id,
            Long profileId
    );

    List<BuilderImprovementIssueEntity> findByProfile_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );

    List<BuilderImprovementIssueEntity>
    findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );

    long countByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
            Long profileId
    );

    long countByProfile_IdAndStatusAndPublishedTrueAndActiveTrueAndDeletedFalse(
            Long profileId,
            BuilderImprovementIssueStatus status
    );
}