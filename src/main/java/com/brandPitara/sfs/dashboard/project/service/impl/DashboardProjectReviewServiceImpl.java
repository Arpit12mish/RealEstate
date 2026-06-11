package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectReviewResponse;
import com.brandPitara.sfs.dashboard.project.dto.ProjectReviewDecisionRequest;
import com.brandPitara.sfs.dashboard.project.dto.ProjectSubmitReviewRequest;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectReviewService;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DashboardProjectReviewServiceImpl implements DashboardProjectReviewService {

    private static final String KEY_PROJECTS = "PROJECTS";
    private static final String KEY_HOME = "HOME";

    private final ProjectRepository projectRepository;
    private final DashboardCurrentUserService currentUserService;
    private final DashboardFieldReviewIssueService fieldReviewIssueService;
    private final DashboardReviewHistoryService reviewHistoryService;
    private final ContentVersionService contentVersionService;

    @Override
    @Transactional(readOnly = true)
    public DashboardProjectReviewResponse getReviewStatus(Long projectId) {
        ProjectEntity project = getProjectOrThrow(projectId);
        return DashboardProjectReviewResponse.from(project);
    }

    @Override
    @Transactional
    public DashboardProjectReviewResponse submitForReview(
            Long projectId,
            ProjectSubmitReviewRequest request
    ) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProjectOrThrow(projectId);

        ReviewStatus oldStatus = normalizeStatus(project.getReviewStatus());

        if (!(oldStatus == ReviewStatus.DRAFT
                || oldStatus == ReviewStatus.RECHECK
                || oldStatus == ReviewStatus.REJECTED)) {
            throw new IllegalStateException(
                    "Project can be submitted only from DRAFT, RECHECK, or REJECTED status. Current status: " + oldStatus
            );
        }

        String remarks = request != null ? clean(request.getRemarks()) : null;

        project.setReviewStatus(ReviewStatus.PENDING_REVIEW);
        project.setSubmittedByDashboardUserId(currentUser.getId());
        project.setSubmittedAt(OffsetDateTime.now());
        project.setReviewedByDashboardUserId(null);
        project.setReviewedAt(null);
        project.setReviewRemarks(remarks);
        project.setPublished(false);

        ProjectEntity saved = projectRepository.save(project);

        reviewHistoryService.record(
                ReviewEntityType.PROJECT,
                saved.getId(),
                oldStatus,
                ReviewStatus.PENDING_REVIEW,
                ReviewActionType.SUBMITTED_FOR_REVIEW,
                remarks,
                currentUser
        );

        contentVersionService.bump(KEY_PROJECTS);

        return DashboardProjectReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public DashboardProjectReviewResponse approve(
            Long projectId,
            ProjectReviewDecisionRequest request
    ) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProjectOrThrow(projectId);

        ReviewStatus oldStatus = normalizeStatus(project.getReviewStatus());
        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().isAdmin();

        if (oldStatus == ReviewStatus.APPROVED) {
            throw new IllegalStateException("Project is already approved.");
        }

        // Reviewers can only act on projects in PENDING_REVIEW; admins can approve from any status.
        if (!isAdmin && oldStatus != ReviewStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Project can be approved only from PENDING_REVIEW status. Current status: " + oldStatus
            );
        }

        boolean hasActiveIssues = fieldReviewIssueService.hasActiveIssues(
                ReviewEntityType.PROJECT,
                projectId
        );

        if (hasActiveIssues) {
            throw new IllegalStateException("Project cannot be approved while active field review issues exist.");
        }

        String remarks = request != null ? clean(request.getRemarks()) : null;

        project.setReviewStatus(ReviewStatus.APPROVED);
        project.setReviewedByDashboardUserId(currentUser.getId());
        project.setReviewedAt(OffsetDateTime.now());
        project.setReviewRemarks(remarks);
        project.setPublished(true);

        ProjectEntity saved = projectRepository.save(project);

        reviewHistoryService.record(
                ReviewEntityType.PROJECT,
                saved.getId(),
                oldStatus,
                ReviewStatus.APPROVED,
                ReviewActionType.APPROVED,
                remarks,
                currentUser
        );

        contentVersionService.bump(KEY_PROJECTS);
        contentVersionService.bump(KEY_HOME);

        return DashboardProjectReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public DashboardProjectReviewResponse rollbackApproval(
            Long projectId,
            ProjectReviewDecisionRequest request
    ) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProjectOrThrow(projectId);

        ReviewStatus oldStatus = normalizeStatus(project.getReviewStatus());

        if (oldStatus != ReviewStatus.APPROVED) {
            throw new IllegalStateException(
                    "Project approval can be rolled back only from APPROVED status. Current status: " + oldStatus
            );
        }

        String remarks = request != null ? clean(request.getRemarks()) : null;

        if (!StringUtils.hasText(remarks)) {
            throw new IllegalArgumentException("Rollback remarks are required");
        }

        project.setReviewStatus(ReviewStatus.PENDING_REVIEW);
        project.setReviewedByDashboardUserId(null);
        project.setReviewedAt(null);
        project.setReviewRemarks(remarks);
        project.setPublished(false);

        ProjectEntity saved = projectRepository.save(project);

        reviewHistoryService.record(
                ReviewEntityType.PROJECT,
                saved.getId(),
                oldStatus,
                ReviewStatus.PENDING_REVIEW,
                ReviewActionType.APPROVAL_ROLLED_BACK,
                remarks,
                currentUser
        );

        contentVersionService.bump(KEY_PROJECTS);
        contentVersionService.bump(KEY_HOME);

        return DashboardProjectReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public DashboardProjectReviewResponse reject(
            Long projectId,
            ProjectReviewDecisionRequest request
    ) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProjectOrThrow(projectId);

        ReviewStatus oldStatus = normalizeStatus(project.getReviewStatus());

        if (!(oldStatus == ReviewStatus.PENDING_REVIEW || oldStatus == ReviewStatus.RECHECK)) {
            throw new IllegalStateException(
                    "Project can be rejected only from PENDING_REVIEW or RECHECK status. Current status: " + oldStatus
            );
        }

        String remarks = request != null ? clean(request.getRemarks()) : null;

        if (!StringUtils.hasText(remarks)) {
            throw new IllegalArgumentException("Reject remarks are required");
        }

        project.setReviewStatus(ReviewStatus.REJECTED);
        project.setReviewedByDashboardUserId(currentUser.getId());
        project.setReviewedAt(OffsetDateTime.now());
        project.setReviewRemarks(remarks);
        project.setPublished(false);

        ProjectEntity saved = projectRepository.save(project);

        reviewHistoryService.record(
                ReviewEntityType.PROJECT,
                saved.getId(),
                oldStatus,
                ReviewStatus.REJECTED,
                ReviewActionType.REJECTED,
                remarks,
                currentUser
        );

        contentVersionService.bump(KEY_PROJECTS);
        contentVersionService.bump(KEY_HOME);

        return DashboardProjectReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public DashboardProjectReviewResponse reopen(Long projectId, ProjectReviewDecisionRequest request) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProjectOrThrow(projectId);

        ReviewStatus oldStatus = normalizeStatus(project.getReviewStatus());

        if (oldStatus != ReviewStatus.REJECTED) {
            throw new IllegalStateException(
                    "Project can be reopened only from REJECTED status. Current status: " + oldStatus
            );
        }

        String remarks = request != null ? clean(request.getRemarks()) : null;

        project.setReviewStatus(ReviewStatus.DRAFT);
        project.setReviewedByDashboardUserId(null);
        project.setReviewedAt(null);
        project.setReviewRemarks(remarks);
        project.setPublished(false);

        ProjectEntity saved = projectRepository.save(project);

        reviewHistoryService.record(
                ReviewEntityType.PROJECT,
                saved.getId(),
                oldStatus,
                ReviewStatus.DRAFT,
                ReviewActionType.REOPENED,
                remarks,
                currentUser
        );

        contentVersionService.bump(KEY_PROJECTS);

        return DashboardProjectReviewResponse.from(saved);
    }

    private ProjectEntity getProjectOrThrow(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required");
        }

        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private ReviewStatus normalizeStatus(ReviewStatus status) {
        return status != null ? status : ReviewStatus.DRAFT;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
