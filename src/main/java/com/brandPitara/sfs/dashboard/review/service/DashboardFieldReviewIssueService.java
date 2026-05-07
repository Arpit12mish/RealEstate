package com.brandPitara.sfs.dashboard.review.service;

import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.review.dto.FieldReviewIssueResponse;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldIssueFixedRequest;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldReviewIssueRequest;

import java.util.List;

public interface DashboardFieldReviewIssueService {

    FieldReviewIssueResponse markIssue(MarkFieldReviewIssueRequest request);

    List<FieldReviewIssueResponse> listIssues(
            ReviewEntityType entityType,
            Long entityId,
            boolean activeOnly
    );

    FieldReviewIssueResponse markFixed(
            Long issueId,
            MarkFieldIssueFixedRequest request
    );

    boolean hasActiveIssues(ReviewEntityType entityType, Long entityId);

    void deleteIssue(Long issueId);
}