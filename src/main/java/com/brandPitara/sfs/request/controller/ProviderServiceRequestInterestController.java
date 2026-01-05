package com.brandPitara.sfs.request.controller;

import com.brandPitara.sfs.request.dto.ServiceRequestInterestResponse;
import com.brandPitara.sfs.request.service.ServiceRequestInterestService;
import com.brandPitara.sfs.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers/me/requests")
@RequiredArgsConstructor
public class ProviderServiceRequestInterestController {

    private final ServiceRequestInterestService interestService;
    private final CurrentUserService currentUserService;

    @PostMapping("/{requestId}/interest")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ServiceRequestInterestResponse interest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String message
    ) {
        return interestService.expressInterest(currentUserService.requireUserId(), requestId, message);
    }
}
