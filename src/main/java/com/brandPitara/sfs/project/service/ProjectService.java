package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.dto.ProjectPatchRequest;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import org.springframework.data.domain.*;

import java.util.List;

public interface ProjectService {
  ProjectResponse create(Long builderId, ProjectUpsertRequest request);
  ProjectResponse update(Long projectId, ProjectUpsertRequest request);
  ProjectResponse patch(Long projectId, ProjectPatchRequest request);
  ProjectResponse setPublished(Long projectId, boolean published);
  ProjectResponse setActive(Long projectId, boolean active);
  ProjectResponse reassignBuilder(Long projectId, Long builderId);
  void softDelete(Long projectId);

  ProjectResponse adminGet(Long projectId);
  Page<ProjectResponse> adminList(Long builderId, Pageable pageable);

  Page<ProjectResponse> dashboardList(Long builderId, ReviewStatus reviewStatus, Pageable pageable);

  Page<ProjectPublicResponse> publicListByBuilder(Long builderId, Pageable pageable);
  ProjectPublicResponse publicGet(Long projectId);
  Page<ProjectPublicResponse> publicFeatured(Long builderId, Pageable pageable);
  Page<ProjectPublicResponse> publicBrowse(List<UnitConfigurationType> unitConfigurations, Long cityId, Pageable pageable);
}
