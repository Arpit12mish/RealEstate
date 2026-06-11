package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;

import java.util.List;

public interface ProjectDetailComposer {
  ProjectResponse compose(ProjectEntity project, List<ProjectMediaEntity> media);
  ProjectPublicResponse composePublic(ProjectEntity project, List<ProjectMediaEntity> media);
  ProjectResponse composeForPreview(ProjectEntity project, List<ProjectMediaEntity> media, ProjectMeterDetailResponse meterDetail);
}