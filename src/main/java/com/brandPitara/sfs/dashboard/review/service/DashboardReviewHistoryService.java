package com.brandPitara.sfs.dashboard.review.service;

import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.review.dto.ReviewHistoryResponse;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;

import java.util.List;

public interface DashboardReviewHistoryService {

    void record(
            ReviewEntityType entityType,
            Long entityId,
            ReviewStatus oldStatus,
            ReviewStatus newStatus,
            ReviewActionType actionType,
            String remarks,
            DashboardUserEntity actionBy
    );

    List<ReviewHistoryResponse> listHistory(ReviewEntityType entityType, Long entityId);
}