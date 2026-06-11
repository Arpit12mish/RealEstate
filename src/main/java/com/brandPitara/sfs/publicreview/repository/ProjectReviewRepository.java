package com.brandPitara.sfs.publicreview.repository;

import com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity;
import com.brandPitara.sfs.publicreview.enums.SfsReviewVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectReviewRepository extends JpaRepository<ProjectReviewEntity, Long> {

    List<ProjectReviewEntity> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(Long projectId);

    List<ProjectReviewEntity> findByProjectIdAndVerificationStatusAndDeletedFalse(
        Long projectId,
        SfsReviewVerificationStatus verificationStatus
    );

    List<ProjectReviewEntity> findByProjectIdAndDisplayStatusAndDeletedFalse(
        Long projectId,
        String displayStatus
    );

    long countByProjectIdAndVerificationStatusAndDeletedFalse(
        Long projectId,
        SfsReviewVerificationStatus verificationStatus
    );

    List<ProjectReviewEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndProjectIdAndSubmittedByUserTrueAndDeletedFalse(Long userId, Long projectId);
}
