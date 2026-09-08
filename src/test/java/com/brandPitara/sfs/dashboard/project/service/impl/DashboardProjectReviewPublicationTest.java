package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.exception.PublicationConflictException;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardProjectReviewPublicationTest {

    private ProjectRepository projects;
    private BuilderRepository builders;
    private DashboardProjectReviewServiceImpl service;
    private ProjectEntity project;
    private BuilderEntity builder;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        builders = mock(BuilderRepository.class);
        DashboardCurrentUserService currentUser = mock(DashboardCurrentUserService.class);
        DashboardFieldReviewIssueService issues = mock(DashboardFieldReviewIssueService.class);
        when(currentUser.getCurrentUserOrThrow()).thenReturn(DashboardUserEntity.builder()
                .id(9L).role(DashboardRole.ADMIN).active(true).build());
        when(issues.hasActiveIssues(any(), any())).thenReturn(false);
        service = new DashboardProjectReviewServiceImpl(
                projects,
                currentUser,
                issues,
                mock(DashboardReviewHistoryService.class),
                mock(ContentVersionService.class),
                mock(ProjectPublicCacheEvictionPublisher.class),
                builders,
                new ProjectPublicVisibilityPolicy());

        builder = BuilderEntity.builder()
                .id(51L).name("Max Estates")
                .published(true).active(true).deleted(false).build();
        project = ProjectEntity.builder()
                .id(130L).name("Project 130").builder(builder)
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .published(false).active(true).deleted(false).build();
        when(projects.findByIdAndDeletedFalse(130L)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        when(builders.findByIdAndDeletedFalseForUpdate(51L)).thenReturn(Optional.of(builder));
    }

    @Test
    void approvalPublishesAtomicallyWhenBuilderIsPublicReady() {
        service.approve(130L, null);

        assertThat(project.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(project.getPublished()).isTrue();
        verify(projects).save(project);
    }

    @Test
    void approvalFailsWithoutPartialMutationWhenBuilderIsUnpublished() {
        builder.setPublished(false);

        assertThatThrownBy(() -> service.approve(130L, null))
                .isInstanceOf(PublicationConflictException.class)
                .satisfies(ex -> assertThat(((PublicationConflictException) ex).getCode())
                        .isEqualTo("PROJECT_BUILDER_NOT_PUBLISHED"));

        assertThat(project.getReviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);
        assertThat(project.getPublished()).isFalse();
        verify(projects, never()).save(any(ProjectEntity.class));
    }
}
