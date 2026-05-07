package com.brandPitara.sfs.dashboard.review.repository;

import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.review.entity.DashboardContentReviewHistoryEntity;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardContentReviewHistoryRepository
        extends JpaRepository<DashboardContentReviewHistoryEntity, Long> {

    List<DashboardContentReviewHistoryEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ReviewEntityType entityType,
            Long entityId
    );

    Page<DashboardContentReviewHistoryEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ReviewEntityType entityType,
            Long entityId,
            Pageable pageable
    );

    Page<DashboardContentReviewHistoryEntity> findByActionByOrderByCreatedAtDesc(
            DashboardUserEntity actionBy,
            Pageable pageable
    );

    Page<DashboardContentReviewHistoryEntity> findByActionTypeOrderByCreatedAtDesc(
            ReviewActionType actionType,
            Pageable pageable
    );
}