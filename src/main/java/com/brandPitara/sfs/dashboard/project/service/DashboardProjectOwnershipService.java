package com.brandPitara.sfs.dashboard.project.service;

public interface DashboardProjectOwnershipService {

    void assignCurrentUserAsCreator(Long projectId);

    void assertCurrentUserCanEditProject(Long projectId);

    void assertCurrentUserCanSubmitProject(Long projectId);
}