package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.project.policy.DashboardProjectAccessPolicy;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardProjectOwnershipServiceImpl implements DashboardProjectOwnershipService {

    private final ProjectRepository projectRepository;
    private final DashboardCurrentUserService currentUserService;
    private final DashboardProjectAccessPolicy accessPolicy;

    @Override
    @Transactional
    public void assignCurrentUserAsCreator(Long projectId) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        ProjectEntity project = getProject(projectId);

        if (project.getCreatedByDashboardUserId() == null) {
            project.setCreatedByDashboardUserId(currentUser.getId());
            projectRepository.save(project);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCurrentUserCanEditProject(Long projectId) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProject(projectId);
        accessPolicy.assertCanEditProject(project, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCurrentUserCanSubmitProject(Long projectId) {
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        ProjectEntity project = getProject(projectId);
        accessPolicy.assertCanSubmitProject(project, currentUser);
    }

    private ProjectEntity getProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required");
        }

        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }
}