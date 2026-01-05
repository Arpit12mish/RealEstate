package com.brandPitara.sfs.request.controller;

import com.brandPitara.sfs.request.dto.ServiceRequestInterestResponse;
import com.brandPitara.sfs.request.service.ServiceRequestInterestService;
import com.brandPitara.sfs.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/requests")
@RequiredArgsConstructor
public class CustomerServiceRequestActionController {

    private final ServiceRequestInterestService interestService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{requestId}/interests")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ServiceRequestInterestResponse> interests(@PathVariable Long requestId) {
        return interestService.listInterestsForCustomer(currentUserService.requireUserId(), requestId);
    }

    @PostMapping("/{requestId}/close")
    @PreAuthorize("hasRole('CUSTOMER')")
    public void close(@PathVariable Long requestId) {
        interestService.closeRequest(currentUserService.requireUserId(), requestId);
    }
}
