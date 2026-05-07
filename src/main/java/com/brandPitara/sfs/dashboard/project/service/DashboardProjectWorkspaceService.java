package com.brandPitara.sfs.dashboard.project.service;

import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectWorkspaceResponse;

public interface DashboardProjectWorkspaceService {

    DashboardProjectWorkspaceResponse getWorkspace(Long projectId);
}
