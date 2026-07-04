package com.brandPitara.sfs.dashboard.builderhighlight.controller;

import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightItemRequest;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightStatusUpdateRequest;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightItemResponse;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.builderhighlight.service.BuilderHighlightService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/builders/{builderId}/highlights")
@RequiredArgsConstructor
public class DashboardBuilderHighlightController {

    private final BuilderHighlightService builderHighlightService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public BuilderHighlightItemResponse create(
        @PathVariable Long builderId,
        @Valid @RequestBody BuilderHighlightItemRequest request
    ) {
        BuilderHighlightItemResponse response = builderHighlightService.create(builderId, request);
        dashboardActionAuditService.record(
            DashboardAuditAction.BUILDER_HIGHLIGHT_CREATED,
            ReviewEntityType.BUILDER_HIGHLIGHT_ITEM,
            response.getId(),
            null
        );
        return response;
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public BuilderHighlightItemResponse update(
        @PathVariable Long builderId,
        @PathVariable Long itemId,
        @Valid @RequestBody BuilderHighlightItemRequest request
    ) {
        BuilderHighlightItemResponse response = builderHighlightService.update(builderId, itemId, request);
        dashboardActionAuditService.record(
            DashboardAuditAction.BUILDER_HIGHLIGHT_UPDATED,
            ReviewEntityType.BUILDER_HIGHLIGHT_ITEM,
            response.getId(),
            null
        );
        return response;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<BuilderHighlightItemResponse> list(
        @PathVariable Long builderId,
        @RequestParam(required = false) BuilderHighlightType type,
        @RequestParam(required = false) BuilderHighlightStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return builderHighlightService.dashboardList(
            builderId,
            type,
            status,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
        );
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public BuilderHighlightItemResponse get(
        @PathVariable Long builderId,
        @PathVariable Long itemId
    ) {
        return builderHighlightService.dashboardGet(builderId, itemId);
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
        @PathVariable Long builderId,
        @PathVariable Long itemId
    ) {
        builderHighlightService.softDelete(builderId, itemId);
        dashboardActionAuditService.record(
            DashboardAuditAction.BUILDER_HIGHLIGHT_DELETED,
            ReviewEntityType.BUILDER_HIGHLIGHT_ITEM,
            itemId,
            null
        );
    }

    @PatchMapping("/{itemId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public BuilderHighlightItemResponse updateStatus(
        @PathVariable Long builderId,
        @PathVariable Long itemId,
        @Valid @RequestBody BuilderHighlightStatusUpdateRequest request
    ) {
        BuilderHighlightItemResponse response = builderHighlightService.updateStatus(builderId, itemId, request);
        dashboardActionAuditService.record(
            DashboardAuditAction.BUILDER_HIGHLIGHT_STATUS_UPDATED,
            ReviewEntityType.BUILDER_HIGHLIGHT_ITEM,
            itemId,
            null
        );
        return response;
    }

    @PatchMapping("/{itemId}/published")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public BuilderHighlightItemResponse setPublished(
        @PathVariable Long builderId,
        @PathVariable Long itemId,
        @RequestParam boolean value
    ) {
        BuilderHighlightItemResponse response = builderHighlightService.setPublished(builderId, itemId, value);
        dashboardActionAuditService.record(
            value ? DashboardAuditAction.BUILDER_HIGHLIGHT_PUBLISHED : DashboardAuditAction.BUILDER_HIGHLIGHT_UNPUBLISHED,
            ReviewEntityType.BUILDER_HIGHLIGHT_ITEM,
            itemId,
            null
        );
        return response;
    }
}
