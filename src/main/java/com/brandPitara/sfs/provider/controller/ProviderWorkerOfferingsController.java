package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.WorkerAvailabilityUpdateRequest;
import com.brandPitara.sfs.provider.dto.WorkerOfferingsResponse;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;
import com.brandPitara.sfs.provider.service.WorkerProfileMutationService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Worker self-service for the Allied Services profile fields. Verification and dealer
 * recommendations are deliberately not editable here.
 */
@RestController
@RequestMapping("/api/providers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WORKER')")
public class ProviderWorkerOfferingsController {

    private final WorkerProfileMutationService workerProfileMutationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/offerings")
    public WorkerOfferingsResponse get() {
        return workerProfileMutationService.get(myWorkerId());
    }

    @PutMapping("/services")
    public WorkerOfferingsResponse replaceServices(@Valid @RequestBody WorkerServicesReplaceRequest request) {
        return workerProfileMutationService.replaceServices(myWorkerId(), request);
    }

    @PutMapping("/rates")
    public WorkerOfferingsResponse replaceRates(@Valid @RequestBody WorkerRatesReplaceRequest request) {
        return workerProfileMutationService.replaceRates(myWorkerId(), request);
    }

    @PatchMapping("/availability")
    public WorkerOfferingsResponse updateAvailability(@Valid @RequestBody WorkerAvailabilityUpdateRequest request) {
        return workerProfileMutationService.updateAvailability(myWorkerId(), request);
    }

    private Long myWorkerId() {
        return workerProfileMutationService.requireWorkerIdForUser(currentUserService.requireUserId());
    }
}
