package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedProjectActionsDtoTest {

    @Test
    void dataEntryOwnerCanSubmitDraftUsingBothSubmitFlagNames() {
        DashboardProjectReviewResponse review = DashboardProjectReviewResponse.builder()
                .projectId(35L)
                .reviewStatus(ReviewStatus.DRAFT)
                .published(false)
                .createdByDashboardUserId(7L)
                .build();

        AllowedProjectActionsDto actions = AllowedProjectActionsDto.compute(
                review,
                user(7L, DashboardRole.DATA_ENTRY)
        );

        assertThat(actions.isCanSubmitReview()).isTrue();
        assertThat(actions.isCanSubmitForReview()).isTrue();
        assertThat(actions.isCanPublish()).isFalse();
    }

    @Test
    void adminCanPublishOnlyApprovedOrAlreadyPublishedProjects() {
        DashboardUserEntity admin = user(1L, DashboardRole.ADMIN);

        AllowedProjectActionsDto draftActions = AllowedProjectActionsDto.compute(
                DashboardProjectReviewResponse.builder()
                        .projectId(35L)
                        .reviewStatus(ReviewStatus.DRAFT)
                        .published(false)
                        .build(),
                admin
        );

        AllowedProjectActionsDto approvedActions = AllowedProjectActionsDto.compute(
                DashboardProjectReviewResponse.builder()
                        .projectId(27L)
                        .reviewStatus(ReviewStatus.APPROVED)
                        .published(false)
                        .build(),
                admin
        );

        AllowedProjectActionsDto inconsistentPublishedActions = AllowedProjectActionsDto.compute(
                DashboardProjectReviewResponse.builder()
                        .projectId(99L)
                        .reviewStatus(ReviewStatus.DRAFT)
                        .published(true)
                        .build(),
                admin
        );

        assertThat(draftActions.isCanPublish()).isFalse();
        assertThat(approvedActions.isCanPublish()).isTrue();
        assertThat(inconsistentPublishedActions.isCanPublish()).isTrue();
    }

    @Test
    void reviewerCanRollbackApprovedProjectButCannotSubmitIt() {
        AllowedProjectActionsDto actions = AllowedProjectActionsDto.compute(
                DashboardProjectReviewResponse.builder()
                        .projectId(31L)
                        .reviewStatus(ReviewStatus.APPROVED)
                        .published(true)
                        .build(),
                user(2L, DashboardRole.REVIEWER)
        );

        assertThat(actions.isCanRollbackApproval()).isTrue();
        assertThat(actions.isCanSubmitReview()).isFalse();
        assertThat(actions.isCanApprove()).isFalse();
        assertThat(actions.isCanPublish()).isFalse();
    }

    private DashboardUserEntity user(Long id, DashboardRole role) {
        DashboardUserEntity user = new DashboardUserEntity();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
