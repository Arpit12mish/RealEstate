package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.*;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectConnectivityController {

  private final ProjectConnectivityService projectConnectivityService;

  @PutMapping("/{projectId}/connectivity")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectConnectivityResponse upsertOverview(@PathVariable Long projectId,
                                                    @RequestBody ProjectConnectivityUpsertRequest request) {
    return projectConnectivityService.upsertOverview(projectId, request);
  }

  @GetMapping("/{projectId}/connectivity")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectConnectivityResponse get(@PathVariable Long projectId) {
    return projectConnectivityService.adminGet(projectId);
  }

  @GetMapping("/{projectId}/connectivity/places")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectConnectivityPlaceResponse> listPlaces(@PathVariable Long projectId) {
    return projectConnectivityService.adminListPlaces(projectId);
  }

  @PostMapping("/{projectId}/connectivity/places")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectConnectivityPlaceResponse addPlace(@PathVariable Long projectId,
                                                   @Valid @RequestBody ProjectConnectivityPlaceUpsertRequest request) {
    return projectConnectivityService.addPlace(projectId, request);
  }

  @PutMapping("/{projectId}/connectivity/places/{placeId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectConnectivityPlaceResponse updatePlace(@PathVariable Long projectId,
                                                      @PathVariable Long placeId,
                                                      @Valid @RequestBody ProjectConnectivityPlaceUpsertRequest request) {
    return projectConnectivityService.updatePlace(projectId, placeId, request);
  }

  @DeleteMapping("/{projectId}/connectivity/places/{placeId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deletePlace(@PathVariable Long projectId, @PathVariable Long placeId) {
    projectConnectivityService.softDeletePlace(projectId, placeId);
  }

  @GetMapping("/connectivity/provider-categories")
  @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
  public List<ConnectivityProviderCategoryMetaResponse> providerCategories() {
    return projectConnectivityService.providerCategories();
  }

  @PostMapping("/{projectId}/connectivity/provider-search")
  @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
  public ConnectivityProviderSearchResponse providerSearch(
      @PathVariable Long projectId,
      @Valid @RequestBody ConnectivityProviderSearchRequest request
  ) {
    return projectConnectivityService.providerSearch(projectId, request);
  }

  @PostMapping("/{projectId}/connectivity/places/bulk")
  @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
  public ProjectConnectivityPlaceBulkSaveResponse bulkSavePlaces(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectConnectivityPlaceBulkUpsertRequest request
  ) {
    return projectConnectivityService.bulkSavePlaces(projectId, request);
  }
}
