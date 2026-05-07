package com.brandPitara.sfs.dashboard.review.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.policy.DashboardProjectAccessPolicy;
import com.brandPitara.sfs.dashboard.review.dto.FieldReviewIssueResponse;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldIssueFixedRequest;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldReviewIssueRequest;
import com.brandPitara.sfs.dashboard.review.entity.DashboardFieldReviewIssueEntity;
import com.brandPitara.sfs.dashboard.review.repository.DashboardFieldReviewIssueRepository;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardFieldReviewIssueServiceImpl implements DashboardFieldReviewIssueService {

    private static final Set<FieldReviewStatus> ACTIVE_ISSUE_STATUSES =
            Set.of(FieldReviewStatus.RECHECK, FieldReviewStatus.WRONG);

    private final DashboardFieldReviewIssueRepository issueRepository;
    private final DashboardCurrentUserService currentUserService;
    private final DashboardReviewHistoryService reviewHistoryService;
    private final ProjectRepository projectRepository;
    private final DashboardProjectAccessPolicy accessPolicy;

    @Override
    @Transactional
    public FieldReviewIssueResponse markIssue(MarkFieldReviewIssueRequest request) {
        validateMarkIssueRequest(request);

        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        String fieldKey = cleanRequired(request.getFieldKey());
        String fieldLabel = clean(request.getFieldLabel());
        String remarks = clean(request.getRemarks());

        DashboardFieldReviewIssueEntity issue = issueRepository
                .findByEntityTypeAndEntityIdAndFieldKeyAndActiveTrue(
                        request.getEntityType(),
                        request.getEntityId(),
                        fieldKey
                )
                .map(existing -> {
                    existing.reopen(request.getStatus(), remarks, currentUser);
                    if (StringUtils.hasText(fieldLabel)) {
                        existing.setFieldLabel(fieldLabel);
                    }
                    return existing;
                })
                .orElseGet(() -> DashboardFieldReviewIssueEntity.builder()
                        .entityType(request.getEntityType())
                        .entityId(request.getEntityId())
                        .fieldKey(fieldKey)
                        .fieldLabel(fieldLabel)
                        .status(request.getStatus())
                        .remarks(remarks)
                        .markedBy(currentUser)
                        .active(true)
                        .build());

        DashboardFieldReviewIssueEntity saved = issueRepository.save(issue);

        ReviewActionType actionType = request.getStatus() == FieldReviewStatus.WRONG
                ? ReviewActionType.FIELD_MARKED_WRONG
                : ReviewActionType.FIELD_MARKED_RECHECK;

        reviewHistoryService.record(
                request.getEntityType(),
                request.getEntityId(),
                null,
                null,
                actionType,
                buildHistoryRemarks(saved),
                currentUser
        );

        if (request.getEntityType() == ReviewEntityType.PROJECT) {
            projectRepository.findByIdAndDeletedFalse(request.getEntityId()).ifPresent(project -> {
                if (project.getReviewStatus() == ReviewStatus.PENDING_REVIEW) {
                    project.setReviewStatus(ReviewStatus.RECHECK);
                    projectRepository.save(project);
                    reviewHistoryService.record(
                            ReviewEntityType.PROJECT,
                            project.getId(),
                            ReviewStatus.PENDING_REVIEW,
                            ReviewStatus.RECHECK,
                            ReviewActionType.MOVED_TO_RECHECK,
                            "Auto-moved to RECHECK due to field issue on: " + saved.getFieldKey(),
                            currentUser
                    );
                }
            });
        }

        return FieldReviewIssueResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldReviewIssueResponse> listIssues(
            ReviewEntityType entityType,
            Long entityId,
            boolean activeOnly
    ) {
        validateEntity(entityType, entityId);

        List<DashboardFieldReviewIssueEntity> issues = activeOnly
                ? issueRepository.findByEntityTypeAndEntityIdAndActiveTrueOrderByMarkedAtDesc(entityType, entityId)
                : issueRepository.findByEntityTypeAndEntityIdOrderByMarkedAtDesc(entityType, entityId);

        return issues.stream()
                .map(FieldReviewIssueResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public FieldReviewIssueResponse markFixed(
            Long issueId,
            MarkFieldIssueFixedRequest request
    ) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue id is required");
        }

        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        DashboardFieldReviewIssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Field review issue not found: " + issueId));

        if (!Boolean.TRUE.equals(issue.getActive()) && issue.getStatus() == FieldReviewStatus.FIXED) {
            return FieldReviewIssueResponse.from(issue);
        }

        if (issue.getEntityType() == ReviewEntityType.PROJECT
                && currentUser.getRole() == DashboardRole.DATA_ENTRY) {
            projectRepository.findByIdAndDeletedFalse(issue.getEntityId())
                    .ifPresentOrElse(
                            project -> accessPolicy.assertCanEditProject(project, currentUser),
                            () -> { throw new NotFoundException("Project not found: " + issue.getEntityId()); }
                    );
        }

        String fixedRemarks = request != null ? clean(request.getRemarks()) : null;

        if (StringUtils.hasText(fixedRemarks)) {
            issue.setRemarks(issue.getRemarks() == null
                    ? fixedRemarks
                    : issue.getRemarks() + "\n\nFix note: " + fixedRemarks);
        }

        issue.markFixed(currentUser);

        DashboardFieldReviewIssueEntity saved = issueRepository.save(issue);

        reviewHistoryService.record(
                saved.getEntityType(),
                saved.getEntityId(),
                null,
                null,
                ReviewActionType.FIELD_MARKED_FIXED,
                buildHistoryRemarks(saved),
                currentUser
        );

        return FieldReviewIssueResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteIssue(Long issueId) {
        if (issueId == null) {
            throw new IllegalArgumentException("Issue id is required");
        }

        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        DashboardFieldReviewIssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Field review issue not found: " + issueId));

        reviewHistoryService.record(
                issue.getEntityType(),
                issue.getEntityId(),
                null,
                null,
                ReviewActionType.FIELD_ISSUE_DELETED,
                "Field issue deleted — field: " + issue.getFieldKey()
                        + (StringUtils.hasText(issue.getFieldLabel()) ? " (" + issue.getFieldLabel() + ")" : ""),
                currentUser
        );

        issueRepository.delete(issue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveIssues(ReviewEntityType entityType, Long entityId) {
        validateEntity(entityType, entityId);

        return issueRepository.existsByEntityTypeAndEntityIdAndActiveTrueAndStatusIn(
                entityType,
                entityId,
                ACTIVE_ISSUE_STATUSES
        );
    }

    private void validateMarkIssueRequest(MarkFieldReviewIssueRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        validateEntity(request.getEntityType(), request.getEntityId());

        if (!StringUtils.hasText(request.getFieldKey())) {
            throw new IllegalArgumentException("Field key is required");
        }

        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Field review status is required");
        }

        if (request.getStatus() == FieldReviewStatus.FIXED) {
            throw new IllegalArgumentException("Use mark fixed endpoint for FIXED status");
        }
    }

    private void validateEntity(ReviewEntityType entityType, Long entityId) {
        if (entityType == null) {
            throw new IllegalArgumentException("Entity type is required");
        }

        if (entityId == null) {
            throw new IllegalArgumentException("Entity id is required");
        }
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);

        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException("Required value is missing");
        }

        return cleaned;
    }

    private String buildHistoryRemarks(DashboardFieldReviewIssueEntity issue) {
        return "Field: " + issue.getFieldKey()
                + (StringUtils.hasText(issue.getFieldLabel()) ? " (" + issue.getFieldLabel() + ")" : "")
                + ", status: " + issue.getStatus()
                + (StringUtils.hasText(issue.getRemarks()) ? ", remarks: " + issue.getRemarks() : "");
    }
}