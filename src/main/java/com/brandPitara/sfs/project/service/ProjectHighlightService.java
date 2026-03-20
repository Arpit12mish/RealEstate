package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.dto.ProjectHighlightUpsertRequest;

import java.util.List;

public interface ProjectHighlightService {
  ProjectHighlightResponse addHighlight(Long projectId, ProjectHighlightUpsertRequest request);
  ProjectHighlightResponse updateHighlight(Long projectId, Long highlightId, ProjectHighlightUpsertRequest request);
  List<ProjectHighlightResponse> adminList(Long projectId);
  List<ProjectHighlightResponse> publicList(Long projectId);
  void softDelete(Long projectId, Long highlightId);
}