package com.brandPitara.sfs.dashboard.builder.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.dto.BuilderUpsertRequest;
import com.brandPitara.sfs.builder.dto.UpdateBuilderLogoRequest;
import com.brandPitara.sfs.builder.service.BuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/builders")
@RequiredArgsConstructor
public class DashboardBuilderController {

    private final BuilderService builderService;
    private final DashboardActionAuditService dashboardActionAuditService;

    /**
     * DATA_ENTRY can add builder data.
     * ADMIN can also add builder data.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public BuilderResponse create(@Valid @RequestBody BuilderUpsertRequest request) {
        BuilderResponse response = builderService.create(request);
        dashboardActionAuditService.record(DashboardAuditAction.BUILDER_CREATED, ReviewEntityType.BUILDER, response.getId(), null);
        return response;
    }

    /**
     * DATA_ENTRY can update builder data while data is being prepared.
     * ADMIN can update anytime.
     *
     * Later, after review fields are added, we will restrict DATA_ENTRY updates
     * to DRAFT / RECHECK / REJECTED records.
     */
    @PutMapping("/{builderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BuilderResponse update(
            @PathVariable Long builderId,
            @Valid @RequestBody BuilderUpsertRequest request
    ) {
        BuilderResponse response = builderService.update(builderId, request);
        dashboardActionAuditService.record(DashboardAuditAction.BUILDER_UPDATED, ReviewEntityType.BUILDER, builderId, null);
        return response;
    }

    /**
     * All dashboard roles can view builder details.
     */
    @GetMapping("/{builderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public BuilderResponse get(@PathVariable Long builderId) {
        return builderService.getById(builderId);
    }

    /**
     * All dashboard roles can list builders.
     * This is needed because DATA_ENTRY must choose builder while creating project.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<BuilderResponse> list(
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by("priority").ascending().and(Sort.by("id").descending())
        );

        return builderService.adminList(published, active, pageable);
    }

    /**
     * DATA_ENTRY can update builder logo after presign upload.
     * ADMIN can also update logo.
     * Intentionally scoped to logoUrl only — does not allow DATA_ENTRY to modify other builder fields.
     */
    @PatchMapping("/{builderId}/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public BuilderResponse updateLogo(
            @PathVariable Long builderId,
            @Valid @RequestBody UpdateBuilderLogoRequest request
    ) {
        BuilderResponse response = builderService.updateLogo(builderId, request);
        dashboardActionAuditService.record(DashboardAuditAction.BUILDER_LOGO_UPDATED, ReviewEntityType.BUILDER, builderId, null);
        return response;
    }

    /**
     * Temporary ADMIN-only direct publish.
     *
     * Later this should be controlled by the approval workflow:
     * approve -> published = true
     */
    @PatchMapping("/{builderId}/published")
    @PreAuthorize("hasRole('ADMIN')")
    public BuilderResponse setPublished(
            @PathVariable Long builderId,
            @RequestParam boolean value
    ) {
        BuilderResponse response = builderService.setPublished(builderId, value);
        dashboardActionAuditService.record(DashboardAuditAction.BUILDER_PUBLISHED, ReviewEntityType.BUILDER, builderId, null);
        return response;
    }

    /**
     * Only ADMIN can delete.
     */
    @DeleteMapping("/{builderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long builderId) {
        builderService.softDelete(builderId);
        dashboardActionAuditService.record(DashboardAuditAction.BUILDER_DELETED, ReviewEntityType.BUILDER, builderId, null);
    }
}