package com.brandPitara.sfs.mobileupdate.controller;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyRequest;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdateEmergencyRequest;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyService;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyAuditResponse;

@RestController
@RequestMapping("/api/dashboard/mobile-app/update-policies")
@RequiredArgsConstructor
public class DashboardMobileAppUpdatePolicyController {

    private final MobileAppUpdatePolicyService service;
    private final MobileAppUpdatePolicyAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<DashboardMobileAppUpdatePolicyResponse> list() {
        return service.list();
    }

    @GetMapping("/{platform}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardMobileAppUpdatePolicyResponse get(@PathVariable MobilePlatform platform) {
        return service.get(platform);
    }

    @GetMapping("/{platform}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Page<MobileAppUpdatePolicyAuditResponse> audit(
            @PathVariable MobilePlatform platform, Pageable pageable) {
        return auditService.list(platform, pageable);
    }

    @PutMapping("/{platform}")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardMobileAppUpdatePolicyResponse update(
            @PathVariable MobilePlatform platform,
            @Valid @RequestBody DashboardMobileAppUpdatePolicyRequest request) {
        return service.update(platform, request);
    }

    @PutMapping("/{platform}/emergency-disable")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardMobileAppUpdatePolicyResponse emergencyDisable(
            @PathVariable MobilePlatform platform,
            @Valid @RequestBody MobileAppUpdateEmergencyRequest request) {
        return service.setEmergencyDisabled(platform, request);
    }
}
