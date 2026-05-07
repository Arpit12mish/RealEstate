package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaUpsertRequest;

import java.util.List;

public interface ProjectMediaService {
  ProjectMediaResponse addMedia(Long projectId, ProjectMediaUpsertRequest request);
  List<ProjectMediaResponse> adminList(Long projectId);
  List<ProjectMediaResponse> publicList(Long projectId);
  ProjectMediaResponse updateMedia(Long projectId, Long mediaId, ProjectMediaUpsertRequest request);
  void softDelete(Long projectId, Long mediaId);
}
