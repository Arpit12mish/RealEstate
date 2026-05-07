package com.brandPitara.sfs.project.policy;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectPublicVisibilityPolicy {

    public boolean isPubliclyVisible(ProjectEntity project) {
        if (project == null) {
            return false;
        }

        return Boolean.TRUE.equals(project.getPublished())
                && Boolean.TRUE.equals(project.getActive())
                && Boolean.FALSE.equals(project.getDeleted())
                && project.getReviewStatus() == ReviewStatus.APPROVED;
    }

    public void assertPubliclyVisible(ProjectEntity project, Long projectId) {
        if (!isPubliclyVisible(project)) {
            throw new NotFoundException("Project not found: " + projectId);
        }
    }
}