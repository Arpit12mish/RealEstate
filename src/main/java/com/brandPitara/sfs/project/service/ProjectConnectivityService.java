package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.*;

import java.util.List;

public interface ProjectConnectivityService {

  ProjectConnectivityResponse adminGet(Long projectId);
  ProjectConnectivityResponse publicGet(Long projectId);

  ProjectConnectivityResponse upsertOverview(Long projectId, ProjectConnectivityUpsertRequest request);

  ProjectConnectivityPlaceResponse addPlace(Long projectId, ProjectConnectivityPlaceUpsertRequest request);
  ProjectConnectivityPlaceResponse updatePlace(Long projectId, Long placeId, ProjectConnectivityPlaceUpsertRequest request);

  List<ProjectConnectivityPlaceResponse> adminListPlaces(Long projectId);

  void softDeletePlace(Long projectId, Long placeId);

  ProjectConnectivitySearchResponse searchNearby(Long projectId, String query);

  ConnectivityProviderSearchResponse providerSearch(Long projectId, ConnectivityProviderSearchRequest request);

  List<ConnectivityProviderCategoryMetaResponse> providerCategories();

  ProjectConnectivityPlaceBulkSaveResponse bulkSavePlaces(Long projectId, ProjectConnectivityPlaceBulkUpsertRequest request);
}
