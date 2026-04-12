package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;

import java.util.List;

public interface ProjectDetailComposer {
  ProjectResponse compose(ProjectEntity project, List<ProjectMediaEntity> media);
}