package com.brandPitara.sfs.dashboard.marketplace.controller;

import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerListItemResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerManagementResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerProfileUpdateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerVerificationRequest;
import com.brandPitara.sfs.dashboard.marketplace.service.DashboardWorkerService;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Allied Services worker management. Verification (public visibility) is ADMIN/REVIEWER only. */
@RestController
@RequestMapping("/api/dashboard/workers")
@RequiredArgsConstructor
public class DashboardWorkerController {

    private final DashboardWorkerService workerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public PageResponse<WorkerListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return workerService.list(q, page, size);
    }

    @GetMapping("/{workerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public WorkerManagementResponse get(@PathVariable Long workerId) {
        return workerService.get(workerId);
    }

    @PatchMapping("/{workerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public WorkerManagementResponse update(@PathVariable Long workerId, @Valid @RequestBody WorkerProfileUpdateRequest request) {
        return workerService.updateProfile(workerId, request);
    }

    @PatchMapping("/{workerId}/verification")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public WorkerManagementResponse verify(@PathVariable Long workerId, @Valid @RequestBody WorkerVerificationRequest request) {
        return workerService.updateVerification(workerId, request);
    }

    @PutMapping("/{workerId}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public WorkerManagementResponse replaceServices(
            @PathVariable Long workerId, @Valid @RequestBody WorkerServicesReplaceRequest request) {
        return workerService.replaceServices(workerId, request);
    }

    @PutMapping("/{workerId}/rates")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public WorkerManagementResponse replaceRates(
            @PathVariable Long workerId, @Valid @RequestBody WorkerRatesReplaceRequest request) {
        return workerService.replaceRates(workerId, request);
    }
}
