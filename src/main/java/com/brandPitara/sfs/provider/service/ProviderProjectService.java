package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.ProviderProjectCreateRequest;
import com.brandPitara.sfs.provider.dto.ProviderProjectResponse;

import java.util.List;

public interface ProviderProjectService {
  ProviderProjectResponse createMyProject(Long currentUserId, ProviderProjectCreateRequest request);
  List<ProviderProjectResponse> listProviderProjects(Long providerId);
  void deleteMyProject(Long currentUserId, Long projectId);
  List<ProviderProjectResponse> listMyProjects(Long currentUserId);

}
