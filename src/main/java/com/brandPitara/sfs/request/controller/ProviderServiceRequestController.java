package com.brandPitara.sfs.request.controller;

import com.brandPitara.sfs.request.dto.ServiceRequestResponse;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;
import com.brandPitara.sfs.request.service.ServiceRequestService;
import com.brandPitara.sfs.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers/me/requests")
@RequiredArgsConstructor
public class ProviderServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public Page<ServiceRequestResponse> feed(
            @RequestParam(required = false, defaultValue = "OPEN") ServiceRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return serviceRequestService.providerFeed(currentUserService.requireUserId(), status, pageable);
    }
}
