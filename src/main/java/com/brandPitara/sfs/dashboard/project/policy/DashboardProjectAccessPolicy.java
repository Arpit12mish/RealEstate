package com.brandPitara.sfs.dashboard.project.policy;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DashboardProjectAccessPolicy {

    private static final Set<ReviewStatus> DATA_ENTRY_EDITABLE_STATUSES = Set.of(
            ReviewStatus.DRAFT,
            ReviewStatus.RECHECK,
            ReviewStatus.REJECTED
    );

    public void assertCanEditProject(ProjectEntity project, DashboardUserEntity currentUser) {
        if (project == null) {
            throw new AccessDeniedException("Project is required");
        }

        if (currentUser == null || currentUser.getRole() == null) {
            throw new AccessDeniedException("Dashboard user is required");
        }

        DashboardRole role = currentUser.getRole();

        if (role == DashboardRole.ADMIN) {
            return;
        }

        if (role != DashboardRole.DATA_ENTRY) {
            throw new AccessDeniedException("Only ADMIN or DATA_ENTRY can edit project data");
        }

        if (!isCreatedByCurrentUser(project, currentUser)) {
            throw new AccessDeniedException("DATA_ENTRY can edit only projects created by themselves");
        }

        ReviewStatus reviewStatus = project.getReviewStatus() != null
                ? project.getReviewStatus()
                : ReviewStatus.DRAFT;

        if (!DATA_ENTRY_EDITABLE_STATUSES.contains(reviewStatus)) {
            throw new AccessDeniedException(
                    "DATA_ENTRY can edit only DRAFT, RECHECK, or REJECTED projects. Current status: " + reviewStatus
            );
        }
    }

    public void assertCanSubmitProject(ProjectEntity project, DashboardUserEntity currentUser) {
        assertCanEditProject(project, currentUser);
    }

    private boolean isCreatedByCurrentUser(ProjectEntity project, DashboardUserEntity currentUser) {
        return project.getCreatedByDashboardUserId() != null
                && currentUser.getId() != null
                && project.getCreatedByDashboardUserId().equals(currentUser.getId());
    }
}