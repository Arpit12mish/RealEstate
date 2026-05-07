package com.brandPitara.sfs.dashboard.review.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.review.dto.ReviewHistoryResponse;
import com.brandPitara.sfs.dashboard.review.entity.DashboardContentReviewHistoryEntity;
import com.brandPitara.sfs.dashboard.review.repository.DashboardContentReviewHistoryRepository;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardReviewHistoryServiceImpl implements DashboardReviewHistoryService {

    private final DashboardContentReviewHistoryRepository historyRepository;

    @Override
    @Transactional
    public void record(
            ReviewEntityType entityType,
            Long entityId,
            ReviewStatus oldStatus,
            ReviewStatus newStatus,
            ReviewActionType actionType,
            String remarks,
            DashboardUserEntity actionBy
    ) {
        historyRepository.save(
                DashboardContentReviewHistoryEntity.of(
                        entityType,
                        entityId,
                        oldStatus,
                        newStatus,
                        actionType,
                        remarks,
                        actionBy
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> listHistory(ReviewEntityType entityType, Long entityId) {
        return historyRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(ReviewHistoryResponse::from)
                .toList();
    }
}