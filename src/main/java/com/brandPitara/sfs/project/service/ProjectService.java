package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import org.springframework.data.domain.*;

public interface ProjectService {
  ProjectResponse create(Long builderId, ProjectUpsertRequest request);
  ProjectResponse update(Long projectId, ProjectUpsertRequest request);
  ProjectResponse setPublished(Long projectId, boolean published);
  ProjectResponse setActive(Long projectId, boolean active);
  void softDelete(Long projectId);

  ProjectResponse adminGet(Long projectId);
  Page<ProjectResponse> adminList(Long builderId, Pageable pageable);

  Page<ProjectResponse> dashboardList(Long builderId, ReviewStatus reviewStatus, Pageable pageable);

  Page<ProjectResponse> publicListByBuilder(Long builderId, Pageable pageable);
  ProjectResponse publicGet(Long projectId);
  Page<ProjectResponse> publicFeatured(Long builderId, Pageable pageable);
}