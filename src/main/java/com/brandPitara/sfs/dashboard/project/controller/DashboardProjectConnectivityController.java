package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceBulkSaveResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceBulkUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityUpsertRequest;
import com.brandPitara.sfs.project.dto.ConnectivityProviderCategoryMetaResponse;
import com.brandPitara.sfs.project.dto.ConnectivityProviderSearchRequest;
import com.brandPitara.sfs.project.dto.ConnectivityProviderSearchResponse;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects")
@RequiredArgsConstructor
public class DashboardProjectConnectivityController {

    private final ProjectConnectivityService projectConnectivityService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    /**
     * Create/update connectivity overview:
     * title, subtitle, map image, active flag.
     */
    @PutMapping("/{projectId}/connectivity")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConnectivityResponse upsertOverview(
            @PathVariable Long projectId,
            @RequestBody ProjectConnectivityUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);

        ProjectConnectivityResponse response =
                projectConnectivityService.upsertOverview(projectId, request);

        dashboardActionAuditService.record(
                DashboardAuditAction.CONNECTIVITY_UPSERTED,
                ReviewEntityType.PROJECT_CONNECTIVITY,
                projectId,
                projectId
        );

        return response;
    }

    /**
     * Get dashboard connectivity overview + places.
     */
    @GetMapping("/{projectId}/connectivity")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectConnectivityResponse get(@PathVariable Long projectId) {
        return projectConnectivityService.adminGet(projectId);
    }

    /**
     * List nearby places/landmarks.
     */
    @GetMapping("/{projectId}/connectivity/places")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectConnectivityPlaceResponse> listPlaces(@PathVariable Long projectId) {
        return projectConnectivityService.adminListPlaces(projectId);
    }

    /**
     * Add nearby place/landmark.
     */
    @PostMapping("/{projectId}/connectivity/places")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConnectivityPlaceResponse addPlace(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectConnectivityPlaceUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConnectivityPlaceResponse response = projectConnectivityService.addPlace(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.CONNECTIVITY_PLACE_ADDED, ReviewEntityType.PROJECT_CONNECTIVITY_PLACE, response.getId(), projectId);
        return response;
    }

    /**
     * Update nearby place/landmark.
     */
    @PutMapping("/{projectId}/connectivity/places/{placeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConnectivityPlaceResponse updatePlace(
            @PathVariable Long projectId,
            @PathVariable Long placeId,
            @Valid @RequestBody ProjectConnectivityPlaceUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConnectivityPlaceResponse response = projectConnectivityService.updatePlace(projectId, placeId, request);
        dashboardActionAuditService.record(DashboardAuditAction.CONNECTIVITY_PLACE_UPDATED, ReviewEntityType.PROJECT_CONNECTIVITY_PLACE, placeId, projectId);
        return response;
    }

    @PatchMapping("/{projectId}/connectivity/places/{placeId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConnectivityPlaceResponse setPlaceActive(
            @PathVariable Long projectId,
            @PathVariable Long placeId,
            @RequestParam boolean value
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConnectivityPlaceResponse response = projectConnectivityService.setPlaceActive(projectId, placeId, value);
        dashboardActionAuditService.record(DashboardAuditAction.CONNECTIVITY_PLACE_UPDATED, ReviewEntityType.PROJECT_CONNECTIVITY_PLACE, placeId, projectId);
        return response;
    }

    /**
     * Only ADMIN can delete nearby place for now.
     */
    @DeleteMapping("/{projectId}/connectivity/places/{placeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePlace(
            @PathVariable Long projectId,
            @PathVariable Long placeId
    ) {
        projectConnectivityService.softDeletePlace(projectId, placeId);
        dashboardActionAuditService.record(DashboardAuditAction.CONNECTIVITY_PLACE_DELETED, ReviewEntityType.PROJECT_CONNECTIVITY_PLACE, placeId, projectId);
    }

    @GetMapping("/connectivity/provider-categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public List<ConnectivityProviderCategoryMetaResponse> providerCategories() {
        return projectConnectivityService.providerCategories();
    }

    @PostMapping("/{projectId}/connectivity/provider-search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ConnectivityProviderSearchResponse providerSearch(
            @PathVariable Long projectId,
            @Valid @RequestBody ConnectivityProviderSearchRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        return projectConnectivityService.providerSearch(projectId, request);
    }

    @PostMapping("/{projectId}/connectivity/places/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConnectivityPlaceBulkSaveResponse bulkSavePlaces(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectConnectivityPlaceBulkUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConnectivityPlaceBulkSaveResponse response = projectConnectivityService.bulkSavePlaces(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.CONNECTIVITY_PLACE_ADDED, ReviewEntityType.PROJECT_CONNECTIVITY_PLACE, projectId, projectId);
        return response;
    }
}
