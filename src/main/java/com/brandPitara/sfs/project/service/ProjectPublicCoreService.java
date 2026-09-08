package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;

public interface ProjectPublicCoreService {
    ProjectPublicCoreData getById(Long projectId);
    ProjectPublicCoreData getBySlug(String projectSlug);
}
