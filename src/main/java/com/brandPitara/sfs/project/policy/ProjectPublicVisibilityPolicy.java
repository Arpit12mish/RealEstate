package com.brandPitara.sfs.project.policy;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.exception.PublicationConflictException;
import org.springframework.stereotype.Component;

@Component
public class ProjectPublicVisibilityPolicy {

    public boolean isPubliclyVisible(ProjectEntity project) {
        return project != null
                && Boolean.TRUE.equals(project.getPublished())
                && isEligibleForPublication(project);
    }

    /** Canonical rule for every transition that can make a project public. */
    public boolean isEligibleForPublication(ProjectEntity project) {
        return project != null
                && Boolean.FALSE.equals(project.getDeleted())
                && Boolean.TRUE.equals(project.getActive())
                && project.getReviewStatus() == ReviewStatus.APPROVED
                && isBuilderPubliclyAvailable(project.getBuilder());
    }

    public boolean isBuilderPubliclyAvailable(BuilderEntity builder) {
        return builder != null
                && Boolean.FALSE.equals(builder.getDeleted())
                && Boolean.TRUE.equals(builder.getActive())
                && Boolean.TRUE.equals(builder.getPublished());
    }

    public void assertEligibleForPublication(ProjectEntity project, Long projectId) {
        assertEligibleForPublication(project, projectId, project != null ? project.getReviewStatus() : null);
    }

    /** Approval targets APPROVED atomically, so evaluate that intended status before mutation. */
    public void assertEligibleForApprovalPublication(ProjectEntity project, Long projectId) {
        assertEligibleForPublication(project, projectId, ReviewStatus.APPROVED);
    }

    private void assertEligibleForPublication(
            ProjectEntity project,
            Long projectId,
            ReviewStatus effectiveReviewStatus
    ) {
        if (project == null || Boolean.TRUE.equals(project.getDeleted())) {
            throw conflict("PROJECT_UNAVAILABLE",
                    "Project " + projectId + " is unavailable and cannot be published.");
        }
        if (!Boolean.TRUE.equals(project.getActive())) {
            throw conflict("PROJECT_INACTIVE",
                    "Project \"" + projectName(project, projectId) + "\" must be active before it can be published.");
        }
        if (effectiveReviewStatus != ReviewStatus.APPROVED) {
            throw conflict("PROJECT_NOT_APPROVED",
                    "Project must be APPROVED before it can be published. Current review status: "
                            + (effectiveReviewStatus != null ? effectiveReviewStatus : ReviewStatus.DRAFT));
        }

        BuilderEntity builder = project.getBuilder();
        if (builder == null || Boolean.TRUE.equals(builder.getDeleted())) {
            throw conflict("PROJECT_BUILDER_UNAVAILABLE",
                    "This project cannot be published because its builder is unavailable.");
        }
        if (!Boolean.TRUE.equals(builder.getActive())) {
            throw conflict("PROJECT_BUILDER_INACTIVE",
                    "This project cannot be published because builder \"" + builderName(builder)
                            + "\" is inactive. Activate the builder first.");
        }
        if (!Boolean.TRUE.equals(builder.getPublished())) {
            throw conflict("PROJECT_BUILDER_NOT_PUBLISHED",
                    "This project cannot be published because builder \"" + builderName(builder)
                            + "\" is not published. Publish the builder first.");
        }
    }

    public void assertPubliclyVisible(ProjectEntity project, Long projectId) {
        if (!isPubliclyVisible(project)) {
            throw new NotFoundException("Project not found: " + projectId);
        }
    }

    // GAP-001: overload for the public-slug-lookup path, mirroring the
    // Long-keyed overload above so the 404 message identifies the slug
    // that was requested rather than a null/unhelpful identifier.
    public void assertPubliclyVisible(ProjectEntity project, String projectSlug) {
        if (!isPubliclyVisible(project)) {
            throw new NotFoundException("Project not found: " + projectSlug);
        }
    }

    private PublicationConflictException conflict(String code, String message) {
        return new PublicationConflictException(code, message);
    }

    private String builderName(BuilderEntity builder) {
        return builder.getName() != null && !builder.getName().isBlank()
                ? builder.getName()
                : "Builder " + builder.getId();
    }

    private String projectName(ProjectEntity project, Long projectId) {
        return project.getName() != null && !project.getName().isBlank()
                ? project.getName()
                : "Project " + projectId;
    }
}
