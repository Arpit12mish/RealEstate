package com.brandPitara.sfs.dashboard.review.repository;

import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.review.entity.DashboardFieldReviewIssueEntity;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DashboardFieldReviewIssueRepository
        extends JpaRepository<DashboardFieldReviewIssueEntity, Long> {

    Optional<DashboardFieldReviewIssueEntity> findByEntityTypeAndEntityIdAndFieldKeyAndActiveTrue(
            ReviewEntityType entityType,
            Long entityId,
            String fieldKey
    );

    List<DashboardFieldReviewIssueEntity> findByEntityTypeAndEntityIdAndActiveTrueOrderByMarkedAtDesc(
            ReviewEntityType entityType,
            Long entityId
    );

    List<DashboardFieldReviewIssueEntity> findByEntityTypeAndEntityIdOrderByMarkedAtDesc(
            ReviewEntityType entityType,
            Long entityId
    );

    Page<DashboardFieldReviewIssueEntity> findByEntityTypeAndEntityIdOrderByMarkedAtDesc(
            ReviewEntityType entityType,
            Long entityId,
            Pageable pageable
    );

    boolean existsByEntityTypeAndEntityIdAndActiveTrueAndStatusIn(
            ReviewEntityType entityType,
            Long entityId,
            Collection<FieldReviewStatus> statuses
    );

    long countByEntityTypeAndEntityIdAndActiveTrueAndStatusIn(
            ReviewEntityType entityType,
            Long entityId,
            Collection<FieldReviewStatus> statuses
    );

    Page<DashboardFieldReviewIssueEntity> findByMarkedByOrderByMarkedAtDesc(
            DashboardUserEntity markedBy,
            Pageable pageable
    );

    Page<DashboardFieldReviewIssueEntity> findByFixedByOrderByFixedAtDesc(
            DashboardUserEntity fixedBy,
            Pageable pageable
    );

    Page<DashboardFieldReviewIssueEntity> findByStatusAndActiveTrueOrderByMarkedAtDesc(
            FieldReviewStatus status,
            Pageable pageable
    );

    long countByActiveTrueAndStatusIn(Collection<FieldReviewStatus> statuses);
}